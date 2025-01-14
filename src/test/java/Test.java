import java.util.Arrays;

public class Test {
  public static void main(String[] args) {
   String fileName = "C:\\Users\\eastw\\Git Projects\\Pers.onal\\filemanager\\1.2.3.xml";
   String[] splitName = fileName.split("\\.");

   String extension = splitName[splitName.length - 1];
   System.out.println(extension);
   String withoutExtension = fileName.replace("." + extension, "");
   System.out.println(withoutExtension);

   String buildName = withoutExtension + "  101." + extension;
   System.out.println(buildName);



}}
