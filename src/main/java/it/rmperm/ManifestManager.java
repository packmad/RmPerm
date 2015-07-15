package it.rmperm;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;


public class ManifestManager {
    private final String manifestPath;
    private Document manifestDoc = null;
    private ArrayList<String> permsOriginal = new ArrayList<>();
    private HashSet<String> permsToRem;

    public ManifestManager(String manifestPath, HashSet<String> permsToRem, boolean listPerms) {
        this.permsToRem = permsToRem;
        this.manifestPath = manifestPath;
        File xmlFile = new File(manifestPath);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            this.manifestDoc = dBuilder.parse(xmlFile);
        } catch (ParserConfigurationException|SAXException|IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        manifestDoc.getDocumentElement().normalize();
        NodeList nList = manifestDoc.getElementsByTagName("uses-permission");
        for (int i=0; i<nList.getLength(); i++) {
            Node nNode = nList.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                String p = eElement.getAttribute("android:name");
                permsOriginal.add(p);
            }
        }
        for (String p : permsToRem)
            removePermission(p);

        if (Main.verboseOutput || listPerms) {
            System.out.println(this);
        }
        if (listPerms) {
            System.exit(0);
        }
        writeToFile();
    }

    public HashSet<String> getPermsToRem() {
        return permsToRem;
    }

    private void removeRECEIVE_BOOT_COMPLETED() {
        String xpathExpr = "/manifest/application/receiver/intent-filter/action[@name='android.intent.action.BOOT_COMPLETED']";
        XPath xPath =  XPathFactory.newInstance().newXPath();
        try {
            NodeList nList = (NodeList) xPath.compile(xpathExpr).evaluate(manifestDoc, XPathConstants.NODESET);
            Node application = (Node) xPath.compile("/manifest/application").evaluate(manifestDoc, XPathConstants.NODE);
            for (int i=0; i<nList.getLength(); i++) {
                Node nNode = nList.item(i);
                application.removeChild(nNode.getParentNode().getParentNode());
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    private void writeToFile() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(manifestDoc);
            StreamResult result = new StreamResult(new File(manifestPath)); // overwrite original manifest
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    public void askRemoval() {
        Scanner scanner = new Scanner(System.in);
        int i=0;
        while (i != -1) {
            System.out.println(this);
            System.out.println("-1 -> exit & confirm");
            System.out.print("Choice: ");
            i = scanner.nextInt();
            if (i == -1)
                break;
            this.removePermission(i);
        }
    }

    private void removePermission(int x) {
        try {
            String permission = permsOriginal.get(x);
            if (permission.equals("android.permission.RECEIVE_BOOT_COMPLETED")) {
                removeRECEIVE_BOOT_COMPLETED();
            }
            permsToRem.add(permission);
            permsOriginal.remove(x);
            String xpathExpr = "/manifest/uses-permission[@name='" + permission + "']";
            XPath xPath =  XPathFactory.newInstance().newXPath();
            Node node = (Node) xPath.compile(xpathExpr).evaluate(manifestDoc, XPathConstants.NODE);
            node.getParentNode().removeChild(node);

        } catch (IndexOutOfBoundsException ioobe) {
            System.err.println("Are you trying to crash me???");
        }
        catch (XPathExpressionException xpee) {
            xpee.printStackTrace();
            System.exit(-1);
        }
    }

    private void removePermission(String p) {
        if (permsOriginal.contains(p)) {
            if (p.equals("android.permission.RECEIVE_BOOT_COMPLETED")) {
                removeRECEIVE_BOOT_COMPLETED();
            }
            else {
                String xpathExpr = "/manifest/uses-permission[@name='" + p + "']";
                XPath xPath =  XPathFactory.newInstance().newXPath();
                Node node = null;
                try {
                    node = (Node) xPath.compile(xpathExpr).evaluate(manifestDoc, XPathConstants.NODE);
                } catch (XPathExpressionException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                node.getParentNode().removeChild(node);
            }
        }
        else {
            System.err.println("You are trying to remove a permission that doesn't exists in the original manifest: " + p);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int i=0;
        sb.append("Current APK permissions:\n");
        for (String s : permsOriginal) {
            sb.append("\t" + i++ + "->" + s + "\n");
        }
        sb.append("Permissions that will be removed:\n");
        for (String s : permsToRem) {
            sb.append("\t" + s + "\n");
        }
        return sb.toString();
    }
}
