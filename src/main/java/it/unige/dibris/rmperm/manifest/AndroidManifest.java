package it.unige.dibris.rmperm.manifest;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class AndroidManifest {
    private final ResSource src;
    private ResChunkHeader fileHeader;
    private ResStringPool stringPool;
    private final List<ResChunk> chunks = new ArrayList<>();
    private final HashMap<String, ResXmlStartElement> permissions = new HashMap<>();

    public AndroidManifest(File source) throws IOException {
        this.src = new ResSource(source);
        read();
    }

    public Iterable<String> getPermissions() {
        return this.permissions.keySet();
    }

    private void read() {
        final int RES_STRING_POOL_TYPE = 0x0001;
        final int RES_XML_START_ELEMENT_TYPE = 0x0102;
        final int RES_XML_END_ELEMENT_TYPE = 0x0103;
        final Stack<ResXmlStartElement> parents = new Stack<>();
        ResXmlStartElement current = null;
        this.fileHeader = new ResChunkHeader(src);
        while (src.position() < fileHeader.size) {
            long bufferStartPos = src.position();
            ResChunkHeader chunkHeader = new ResChunkHeader(src);
            if (chunkHeader.type == RES_STRING_POOL_TYPE) {
                ResStringPoolHeader poolHeader = new ResStringPoolHeader(chunkHeader, src);
                assert this.stringPool == null;
                this.stringPool = new ResStringPool(poolHeader, src);
            } else if (chunkHeader.type == RES_XML_START_ELEMENT_TYPE) {
                ResXmlStartElement startElement = new ResXmlStartElement(chunkHeader, src);
                parents.push(current);
                current = startElement;
                this.chunks.add(startElement);
            } else if (chunkHeader.type == RES_XML_END_ELEMENT_TYPE) {
                ResXmlEndElement endElement = new ResXmlEndElement(chunkHeader, src);
                assert current!=null;
                assert current.name.lookup().equals(endElement.name.lookup());
                current.setEndElement(endElement);
                current = parents.pop();
                this.chunks.add(endElement);
            } else {
                UnknownResource unknown = new UnknownResource(chunkHeader, src);
                this.chunks.add(unknown);
            }
            src.position(bufferStartPos + chunkHeader.size);
        }
    }

    public void write(File file) throws IOException {
        ResTarget tgt = new ResTargetImpl(file);
        writeTo(tgt);
        tgt.close();
    }

    private void writeTo(ResTarget tgt) throws IOException {
        this.fileHeader.writeTo(tgt);
        long startPos = tgt.position();
        this.stringPool.writeTo(tgt);
        for (ResChunk chunk : chunks)
            chunk.writeTo(tgt);
        long length = tgt.position() - startPos;
        tgt.position(0);
        long newLength = fileHeader.writeTo(tgt, (int) length);
        tgt.position(newLength);
    }

    public boolean tryToRemovePermission(String permission) {
        ResXmlStartElement element = permissions.get(permission);
        if (element == null) {
            return false;
        }
        permissions.remove(permission);
        element.remove();
        return true;
    }

    private interface ResChunk {
        void writeTo(ResTarget tgt) throws IOException;
    }

    private static class ResChunkHeader {
        private final long chunkOriginalStart;
        private final int type;  //u16
        private final int headerSize;  //u16
        private final long size; //u32

        public ResChunkHeader(ResSource src) {
            chunkOriginalStart = src.position();
            type = src.readU16();
            headerSize = src.readU16();
            size = src.readU32();
        }

        public long writeTo(ResTarget tgt, long newSize) throws IOException {
            tgt.writeU16(type);
            tgt.writeU16(headerSize);
            long newSizeAligned = newSize + 2 * ResTarget.LEN_U16 + ResTarget.LEN_U32;
            while (0 != newSizeAligned % 4) {
                newSizeAligned++;
            }
            tgt.writeU32(newSizeAligned);
            return newSizeAligned;
        }

        public void writeTo(ResTarget tgt) throws IOException {
            tgt.writeU16(type);
            tgt.writeU16(headerSize);
            tgt.writeU32(size);
        }

        @Override
        public String toString() {
            return "ResChunkHeader{type=" + type + ", headerSize=" + headerSize + ", size=" + size + "}";
        }
    }

    private static class ResStringPoolHeader {
        private final ResChunkHeader header;
        private final long stringCount; // u32
        private final long styleCount; // u32
        private final long flags; // u32
        private final long stringsStart; // u32
        private final long stylesStart; // u32

        private ResStringPoolHeader(ResChunkHeader header, ResSource src) {
            this.header = header;
            this.stringCount = src.readU32();
            this.styleCount = src.readU32();
            this.flags = src.readU32();
            this.stringsStart = src.readU32();
            this.stylesStart = src.readU32();
        }

        @Override
        public String toString() {
            return "ResStringPoolHeader{" +
                    "header=" + header +
                    ", stringCount=" + stringCount +
                    ", styleCount=" + styleCount +
                    ", flags=" + flags +
                    ", stringsStart=" + stringsStart +
                    ", stylesStart=" + stylesStart +
                    '}';
        }

        public void writeTo(ResTarget tgt, int newStringCount, int newStringsStart, int stylesStart, int length) throws IOException {
            header.writeTo(tgt, length + 5 * ResTarget.LEN_U32);
            tgt.writeU32(newStringCount);
            tgt.writeU32(styleCount);
            tgt.writeU32(flags);
            tgt.writeU32(newStringsStart);
            tgt.writeU32(stylesStart);
        }
    }

    private class ResStringPool implements ResChunk {
        private final ResStringPoolHeader header;
        private final long[] styleOffsets;
        private final List<String> strings = new ArrayList<>();
        private final byte[] stylesData;

        public void writeTo(ResTarget tgt) throws IOException {
            int stringsStart = header.header.headerSize + strings.size() * ResTarget.LEN_U32 + styleOffsets.length * ResTarget.LEN_U32;
            int stylesStart = stringsStart;
            for (String st : strings)
                stylesStart += (2 + st.length()) * 2;
            int length = stylesStart + stylesData.length;
            header.writeTo(tgt, strings.size(), stringsStart, header.stylesStart > 0 ? stylesStart : 0, length - header.header.headerSize);
            int pos = 0;
            for (final String st : strings) {
                tgt.writeU32(pos);
                pos += (2 + st.length()) * 2;
            }
            int styleShift = (int) (stylesStart - header.stylesStart);
            for (long styleOffset : styleOffsets)
                tgt.writeU32(styleOffset + styleShift);
            for (final String st : strings) {
                int stLen = st.length();
                if (stLen > 32767) {
                    int high = (stLen >> 16) & (0x8000);
                    int low = stLen & 0xffff;
                    tgt.writeU8(high);
                    tgt.writeU8(low);
                } else
                    tgt.writeU16(stLen);
                for (char c : st.toCharArray())
                    tgt.writeU16(c);
                tgt.writeU16(0);
            }
            tgt.write(stylesData);
            while (tgt.position() % 4 != 0)
                tgt.writeU8(0);
        }

        public ResStringPool(ResStringPoolHeader header, ResSource src) {
            this.header = header;
            long stringOffsets[] = new long[(int) header.stringCount];
            this.styleOffsets = new long[(int) header.styleCount];
            for (int i = 0; i < stringOffsets.length; i++)
                stringOffsets[i] = src.readU32();
            for (int i = 0; i < this.styleOffsets.length; i++)
                this.styleOffsets[i] = src.readU32();
            long startOfStrings = header.header.chunkOriginalStart + header.stringsStart;
            for (long stringOffset : stringOffsets) {
                src.position(startOfStrings + stringOffset);
                int size = src.readU16();
                if (size > 32767)
                    size = (size & 0x7fff) << 16 + src.readU16();
                char[] c = new char[size];
                src.asCharBuffer().get(c);
                this.strings.add(String.valueOf(c));
            }
            if (header.styleCount > 0) {
                long startOfStyles = header.header.chunkOriginalStart + header.stylesStart;
                long endOfStyles = header.header.chunkOriginalStart + header.header.size;
                this.stylesData = new byte[(int) (endOfStyles - startOfStyles)];
                src.position((int) startOfStrings);
                src.get(this.stylesData);
            } else
                this.stylesData = new byte[0];
        }

        @Override
        public String toString() {
            return "ResStringPool{" +
                    "header=" + header +
                    ", styleOffsets=" + Arrays.toString(styleOffsets) +
                    ", strings=" + strings +
                    '}';
        }

        public String lookup(long index) {
            if (index >= 0 && index < strings.size())
                return strings.get((int) index);
            return "<UNKNOWN>";
        }
    }

    private class ResStringPoolRef {
        private final long index; // u32

        public ResStringPoolRef(ResSource src) {
            this.index = src.readU32();
        }

        public void writeTo(ResTarget tgt) throws IOException {
            tgt.writeU32(index);
        }

        @Override
        public String toString() {
            return "ResStringPoolRef{" +
                    ", index=" + index +
                    ", val=" +
                    lookup() +
                    '}';
        }

        public String lookup() {
            return (4294967295L == index ? "<NONE>" : stringPool.lookup(index));
        }
    }

    private class UnknownResource implements ResChunk {
        private final ResChunkHeader header;
        private final long contentDataSize;

        public UnknownResource(ResChunkHeader header, ResSource src) {
            long startOfContent = src.position();
            this.header = header;
            this.contentDataSize = header.size - (startOfContent - header.chunkOriginalStart);
        }

        @Override
        public void writeTo(ResTarget tgt) throws IOException {
            header.writeTo(tgt);
            src.position(header.chunkOriginalStart);
            new ResChunkHeader(src);
            src.copyTo(contentDataSize, tgt);
        }

        @Override
        public String toString() {
            return "UnknownResource{" +
                    "header=" + header +
                    ", contentDataSize=" + contentDataSize +
                    '}';
        }
    }

    private class ResXmlTreeNodeHeader {
        private final ResChunkHeader header;
        private final long sourceLineNumber; // u32
        private final ResStringPoolRef comment;

        public ResXmlTreeNodeHeader(ResChunkHeader header, ResSource src) {
            this.header = header;
            this.sourceLineNumber = src.readU32();
            this.comment = new ResStringPoolRef(src);
            src.position(header.chunkOriginalStart + header.headerSize);
        }

        public void writeTo(ResTarget tgt) throws IOException {
            header.writeTo(tgt);
            tgt.writeU32(sourceLineNumber);
            comment.writeTo(tgt);
        }

        @Override
        public String toString() {
            return "ResXmlTreeNodeHeader{" +
                    "header=" + header +
                    ", sourceLineNumber=" + sourceLineNumber +
                    ", comment=" + comment +
                    '}';
        }
    }

    private class ResValue {
        private final int size; // u16
        private final int reserved; // u8
        private final int type; // u8
        private final long data; // u32

        private ResValue(ResSource src) {
            this.size = src.readU16();
            this.reserved = src.readU8();
            this.type = src.readU8();
            this.data = src.readU32();
        }

        public void writeTo(ResTarget tgt) throws IOException {
            tgt.writeU16(size);
            tgt.writeU8(reserved);
            tgt.writeU8(type);
            tgt.writeU32(data);
        }

        @Override
        public String toString() {
            String val = "";
            switch (type) {
                case 3:
                    val = "(str:" + stringPool.lookup(data) + ")";
                    break;
                case 0x10:
                    val = "(dec:" + Long.toString(data) + ")";
            }
            return "ResValue{" +
                    "size=" + size +
                    ", reserved=" + reserved +
                    ", type=" + type +
                    ", data=" + data + val +
                    '}';
        }

        public String asString() {
            if (3 != type) {
                throw new IllegalStateException();
            }
            return stringPool.lookup(data);
        }

        public int asInt() {
            if (0x10 != type) {
                throw new IllegalStateException();
            }
            return (int) this.data;
        }
    }

    private class ResXmlAttribute {
        private final ResStringPoolRef namespace;
        private final ResStringPoolRef name;
        private final ResStringPoolRef rawValue;
        private final ResValue typedValue;

        private ResXmlAttribute(ResSource src) {
            this.namespace = new ResStringPoolRef(src);
            this.name = new ResStringPoolRef(src);
            this.rawValue = new ResStringPoolRef(src);
            this.typedValue = new ResValue(src);
        }

        public void writeTo(ResTarget tgt) throws IOException {
            this.namespace.writeTo(tgt);
            this.name.writeTo(tgt);
            this.rawValue.writeTo(tgt);
            typedValue.writeTo(tgt);
        }

        @Override
        public String toString() {
            return "\n\tResXmlAttribute{" +
                    "namespace=" + namespace +
                    ", name=" + name +
                    ", rawValue=" + rawValue +
                    ", typedValue=" + typedValue +
                    '}';
        }
    }

    private class ResXmlStartElement implements ResChunk {
        private final ResXmlTreeNodeHeader header;
        private final ResStringPoolRef namespace;
        private final ResStringPoolRef name;
        private final int attributeStart; // u16
        private final int attributeSize; // u16
        private final int attributeCount; // u16
        private final int idAttributeIndex; // u16
        private final int classAttributeIndex; // u16
        private final int styleAttributeIndex; // u16
        private final ResXmlAttribute[] attributes;
        private ResXmlEndElement endElement;

        public void setEndElement(ResXmlEndElement endElement) {
            assert this.endElement == null;
            this.endElement = endElement;
        }

        public void remove() {
            assert this.endElement != null;
            assert this.name.lookup().equals(this.endElement.name.lookup());
            AndroidManifest.this.chunks.remove(this);
            AndroidManifest.this.chunks.remove(this.endElement);
        }

        @Override
        public void writeTo(ResTarget tgt) throws IOException {
            header.writeTo(tgt);
            namespace.writeTo(tgt);
            name.writeTo(tgt);
            tgt.writeU16(this.attributeStart);
            tgt.writeU16(this.attributeSize);
            tgt.writeU16(this.attributeCount);
            tgt.writeU16(this.idAttributeIndex);
            tgt.writeU16(this.classAttributeIndex);
            tgt.writeU16(this.styleAttributeIndex);
            for (int i = 0; i < this.attributeCount; i++)
                this.attributes[i].writeTo(tgt);
        }

        public ResXmlStartElement(ResChunkHeader chunkHeader, ResSource src) {
            this.header = new ResXmlTreeNodeHeader(chunkHeader, src);
            this.namespace = new ResStringPoolRef(src);
            this.name = new ResStringPoolRef(src);
            this.attributeStart = src.readU16();
            this.attributeSize = src.readU16();
            this.attributeCount = src.readU16();
            this.idAttributeIndex = src.readU16();
            this.classAttributeIndex = src.readU16();
            this.styleAttributeIndex = src.readU16();

            final boolean isPermission = this.name.lookup().equalsIgnoreCase("uses-permission");
            this.attributes = new ResXmlAttribute[this.attributeCount];
            for (int i = 0; i < this.attributeCount; i++) {
                final ResXmlAttribute attribute = new ResXmlAttribute(src);
                this.attributes[i] = attribute;
                if (isPermission) {
                    final String name = attribute.name.lookup();
                    if (name.equalsIgnoreCase("name")) {
                        permissions.put(attribute.typedValue.asString(), this);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "ResXmlStartElement{" +
                    //"header=" + header +
                    //", namespace=" + namespace +
                    //", name=" +
                    name.lookup() +
                    /*", attributeStart=" + attributeStart +
                    ", attributeSize=" + attributeSize +
                    ", attributeCount=" + attributeCount +
                    ", idAttributeIndex=" + idAttributeIndex +
                    ", classAttributeIndex=" + classAttributeIndex +
                    ", styleAttributeIndex=" + styleAttributeIndex +
                    ", attributes=" + Arrays.toString(attributes) +*/
                    '}';
        }
    }

    private class ResXmlEndElement implements ResChunk {
        private final ResXmlTreeNodeHeader header;
        private final ResStringPoolRef namespace;
        private final ResStringPoolRef name;

        @Override
        public void writeTo(ResTarget tgt) throws IOException {
            header.writeTo(tgt);
            namespace.writeTo(tgt);
            name.writeTo(tgt);
        }

        public ResXmlEndElement(ResChunkHeader chunkHeader, ResSource src) {
            this.header = new ResXmlTreeNodeHeader(chunkHeader, src);
            this.namespace = new ResStringPoolRef(src);
            this.name = new ResStringPoolRef(src);
        }

        @Override
        public String toString() {
            return "ResXmlEndElement{" +
                    //"header=" + header +
                    //", namespace=" + namespace +
                    //", name=" +
                    name.lookup() +
                    '}';
        }
    }

    public static class ResSource {
        final RandomAccessFile sourceFile;
        final MappedByteBuffer buffer;

        public ResSource(File source) throws IOException {
            this.sourceFile = new RandomAccessFile(source, "r");
            FileChannel inChannel = this.sourceFile.getChannel();
            this.buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        public int readU8() {
            return buffer.get() & 0xff;
        }

        public int readU16() {
            return (int) (buffer.getShort()) & 0xffff;
        }

        public long readU32() {
            return (long) buffer.getInt() & 0xffffffffL;
        }

        public long position() {
            return buffer.position();
        }

        public void position(long l) {
            buffer.position((int) l);
        }

        public CharBuffer asCharBuffer() {
            return buffer.asCharBuffer();
        }

        public void get(byte[] data) {
            buffer.get(data);
        }

        public void copyTo(long len, ResTarget tgt) throws IOException {
            for (long i = 0; i < len; i++) {
                tgt.writeU8(this.readU8());
            }
        }
    }

    public interface ResTarget {
        int LEN_U32 = 4;
        int LEN_U16 = 2;
        int LEN_U8 = 1;

        void close() throws IOException;

        long position() throws IOException;

        void position(long position) throws IOException;

        void writeU16(int val) throws IOException;

        void writeU32(long val) throws IOException;

        void writeU8(int val) throws IOException;

        void write(byte[] data) throws IOException;
    }

    public static class ResTargetImpl implements ResTarget {
        private RandomAccessFile file;
        private final File outputFile;

        public ResTargetImpl(File file) throws IOException {
            outputFile = file;
            this.file = new RandomAccessFile(outputFile, "rw");
        }

        @Override
        public void close() throws IOException {
            file.close();
        }

        @Override
        public long position() throws IOException {
            return this.file.getFilePointer();
        }

        @Override
        public void position(long position) throws IOException {
            this.file.seek(position);
        }

        @Override
        public void writeU16(int val) throws IOException {
            this.file.write(val & 0xff);
            this.file.write((val >>> 8) & 0xff);
        }

        @Override
        public void writeU32(long val) throws IOException {
            this.file.write((int) (val & 0xff));
            this.file.write((int) ((val >>> 8) & 0xff));
            this.file.write((int) ((val >>> 16) & 0xff));
            this.file.write((int) ((val >>> 24) & 0xff));
        }

        @Override
        public void writeU8(int val) throws IOException {
            this.file.write(val & 0xff);
        }

        @Override
        public void write(byte[] data) throws IOException {
            this.file.write(data);
        }
    }

}