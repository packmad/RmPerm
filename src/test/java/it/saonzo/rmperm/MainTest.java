package it.saonzo.rmperm;

public class MainTest {
    /*
    private static final File outputFolder = new File("./src/test/resources/output/");
    private static final File outputFile = new File(outputFolder, "output.apk");
    private static final File customDex = new File("./src/test/resources/custom.dex");
    private static final File apk4Test = new File("/home/simo/Downloads/merged.apk"); //TODO


    private static String[] fromListToArray(List<String> stringList) {
        String[] ret = new String[stringList.size()];
        ret = stringList.toArray(ret);
        return ret;
    }


    @Before
    public void setUp() throws Exception {
        if (outputFile.exists())
            Assert.assertTrue(outputFile.delete());
        Assert.assertTrue(outputFolder.exists());
        Assert.assertTrue(customDex.exists());
        Assert.assertTrue(apk4Test.exists());
    }


    @After
    public void tearDown() throws Exception {
        Assert.assertTrue(outputFile.delete());
    }


    @Test
    public void mainOnlyPermissions() throws Exception {
        List<String> args = new ArrayList<>();
        args.add("--remove");
        args.add("--input");
        args.add(apk4Test.toString());
        args.add("--custom-methods");
        args.add(customDex.toString());
        args.add("--permissions");
        args.add("ACCESS_COARSE_LOCATION");
        args.add("--output");
        args.add(outputFile.toString());
        args.add("--debug");
        Main.main(fromListToArray(args));
        Assert.assertTrue(outputFile.exists());
    }


    @Test
    public void mainOnlyAds() throws Exception {
        List<String> args = new ArrayList<>();
        args.add("--removeads");
        args.add("--input");
        args.add(apk4Test.toString());
        args.add("--output");
        args.add(outputFile.toString());
        args.add("--debug");
        Main.main(fromListToArray(args));
        Assert.assertTrue(outputFile.exists());
    }
*/
}