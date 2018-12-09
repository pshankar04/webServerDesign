import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class TestServer {

	 

	public static void main(String[] args) throws IOException {

		String dir = "./public/a3-test/";
//		String filename = "index.html";
		File currentDir = new File(dir);
		//System.out.println(displayDirectoryContents(currentDir,filename));
		//String content = getChunkedBytes("./public/a3-test/404.html");
		//System.out.println(content);
		
		
		
		if(currentDir.exists()){
			File file =new File("../public/a3-test/index.html.ja");
			
			double bytes = file.length();
			double kilobytes = (bytes / 1024);
			double megabytes = (kilobytes / 1024);
			double gigabytes = (megabytes / 1024);
			double terabytes = (gigabytes / 1024);
			double petabytes = (terabytes / 1024);
			double exabytes = (petabytes / 1024);
			double zettabytes = (exabytes / 1024);
			double yottabytes = (zettabytes / 1024);
			
			System.out.println("bytes : " + file.isFile() + " "+file.getCanonicalPath());
			System.out.println("bytes : " + bytes);
			System.out.println("kilobytes : " + kilobytes);
			System.out.println("megabytes : " + megabytes);
			System.out.println("gigabytes : " + gigabytes);
			System.out.println("terabytes : " + terabytes);
			System.out.println("petabytes : " + petabytes);
			System.out.println("exabytes : " + exabytes);
			System.out.println("zettabytes : " + zettabytes);
			System.out.println("yottabytes : " + yottabytes);
		}
	 
	}
	
 
	public static byte[] readByteBlock(InputStream in, int offset, int noBytes) throws IOException {
	    byte[] result = new byte[noBytes];
	    in.read(result, offset, noBytes);
	    return result;
	}
	
	public static String getChunkedBytes(String fileName) throws IOException{
		String chuckedContent = "\n";
		File file = new File(fileName);
		System.out.println("SIZE :"+file.length());
		InputStream in = new FileInputStream(file);
		int chunkSize = 16;
		for(int i = 0 ; i <= file.length()+8 ; i+=chunkSize ){
			chuckedContent =   chuckedContent  + "\n"+ Integer.toHexString(i) + "\n" +new String(readByteBlock(in,0,chunkSize-1)) ;
		}
		System.out.println("chuckedContent :"+chuckedContent);
		return chuckedContent;
	}
	public static boolean displayDirectoryContents(File dir,String filename) {
		try {
			File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					System.out.println("directory:" + file.getCanonicalPath());
					displayDirectoryContents(file,filename);
				} else {
					if(file.getName().equals(filename)){
						return true;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	

}
