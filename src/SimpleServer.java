

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


public class SimpleServer {
	public static String method = "";
	public static String fileRequested = "";
	public static String httpVersion = "";
	public static String host="";
	public static String connection="";
	public static boolean placeholderTest = false;
	public static String WEBROOT= "./public";
	public static ArrayList<String> allowList = new ArrayList<String>();
	public static BufferedReader in;
	public static PrintWriter out;
	public static DataOutputStream dataOut;
	public static boolean isFileAvailable = false;


	public static void main(String args[]) {

		allowList.add("GET");
		allowList.add("HEAD");
		allowList.add("OPTIONS");

		try {
			int port = Integer.parseInt(args[0]);
			LinkedHashMap<String,String> headerMap = new LinkedHashMap<String,String>();
			ServerSocket ss = new ServerSocket(port);

			for (;;) {

				Socket client = ss.accept();
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				out = new PrintWriter(client.getOutputStream());
				dataOut = new DataOutputStream(client.getOutputStream());

				String line;
				int count =0;
				boolean malformedHeader = false;
				boolean incorrectVersion = false;
				boolean missingHost = false;

				while ((line = in.readLine()) != null) {
					System.out.println("LINE : "+line);
					if (line.length() == 0)
						break;
					else{


						String[] requestHeader = line.split(" ");
						if(count == 0 && requestHeader.length == 3){					 
							headerMap.put("method",requestHeader[0]);
							headerMap.put("fileRequested",requestHeader[1]);
							headerMap.put("httpVersion",requestHeader[2]);

						}else if(line.contains(":") && count > 0) {
							headerMap.put(line.split(": ")[0],line.split(": ")[1]);	
						}

						else {
							malformedHeader = true;
							continue;				
						}
						count++;
					}
				}
				if(count < 3){
					malformedHeader = true;
				}

				Iterator<Map.Entry<String, String>> it = headerMap.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String,String> pair = (Map.Entry<String,String>)it.next();
					if(pair.getKey().equals("method")){
						method = (String)pair.getValue();
					}else if(pair.getKey().equals("fileRequested")){
						fileRequested = (String)pair.getValue();
					}else if(pair.getKey().equals("httpVersion")){
						httpVersion = (String)pair.getValue();
					}else if(pair.getKey().equals("Host")){
						host = (String)pair.getValue();
						if(host.equals("")){
							System.out.println("NOTHING :");
						}
					}else if(pair.getKey().equals("Connection")){
						connection = (String)pair.getValue();
					}
				}

				System.out.println("method:"+method);
				System.out.println("fileRequested:"+fileRequested);
				System.out.println("httpVersion:"+httpVersion);
				System.out.println("host:"+host);
				System.out.println("connection:"+connection);

				fileRequested = java.net.URLDecoder.decode(fileRequested, "UTF-8");

				if(!httpVersion.equals("HTTP/1.1")){
					incorrectVersion = true;
				}

				if(host.equals("")){
					missingHost = true;
				}
				if(method.equals("TRACE")){

				}
				
				if (fileRequested.endsWith("/") && (fileRequested.indexOf('/') >= 0) && (fileRequested.lastIndexOf('/') > 0 )){
					fileRequested = fileRequested +"index.html";
				}
				else if(fileRequested.equals("/")){
					fileRequested = "/";
				}
				else if (fileRequested.endsWith("/")) {
					fileRequested = "index.html";
				}
				else{
					if(fileRequested.contains("//")){
						fileRequested = fileRequested.substring(fileRequested.indexOf("//")+2);	
						fileRequested = fileRequested.substring(fileRequested.indexOf('/'));
					}else{
						fileRequested = fileRequested.substring(fileRequested.indexOf('/'));
					}

				}
				fileRequested = WEBROOT + fileRequested;
				System.out.println("file Requested  123:"+fileRequested);
				File file = new File(fileRequested);
				String fileLength = String.valueOf(file.length());				
				String content = getContentType(fileRequested,method);

				String formatted = getServerTime();

				if (method.equals("GET")) { 					
					boolean isFileAvailable = checkAvailability(fileRequested);
					System.out.println("Is Available : "+isFileAvailable);
				
					if(malformedHeader || missingHost){
						out.print("HTTP/1.1 400 Bad Request Error \r\n");  									 
						out.println("Date: " +formatted+"\r\n"); 
						out.print("Connection: close\r\n");  
						out.print("\r\n\r\n");						
					}
					else if(incorrectVersion){
						out.print("HTTP/1.1 505 Version Not Supported \r\n");  									 
						out.println("Date: " +formatted+"\r\n"); 
						out.print("Connection: close\r\n");  
						out.print("\r\n\r\n");						
					}
					else if(missingHost){
						out.print("HTTP/1.1 400 Bad Request Error\r\n");  										 
						out.println("Date: " +formatted+"\r\n"); 
						out.print("Connection: close\r\n");  
						out.print("\r\n\r\n");						
					}

					else {
						long newfileLength = 0l;
						boolean verifiedCaseFile = false;

						if(isFileAvailable){
							File fileNow = null;
							String fileNewName = "";
 
							System.out.println("File Req :"+fileRequested);
							if(!fileRequested.equals("./public/")){
								verifiedCaseFile = verifyCaseSensitiveFiles(fileRequested);
							}else{
								fileRequested = "/public/" +"index.html";
								verifiedCaseFile = true;
							}
							if(verifiedCaseFile){

								String filePath = new File(fileRequested).getAbsolutePath().replace("./","");
								System.out.println("filePath : "+filePath);
								File fileForLength = new File(filePath);
								newfileLength = fileForLength.length();

								String str = "HTTP/1.1 200 OK\r\n"+
										"Date: " +formatted+"\r\n" +
										"Server :localhost:8090"+"\r\n"+
										"Content-Type: "+content+"\r\n"+
										"Last-Modified :"+getLastModified(filePath)+"\r\n"+
										"Accept-Ranges : bytes"+"\r\n"+
										"Content-Length:"+newfileLength+"\r\n"+											 
										"Connection: close"+"\r\n\r\n";

								dataOut.write(str.getBytes());

								if(newfileLength != 0){
									FileReader fr = new FileReader(fileRequested);
									BufferedReader br = new BufferedReader(fr);
									String fileLine;
									while ((fileLine = br.readLine()) != null) {
										dataOut.write((fileLine).getBytes());								
									}
									dataOut.flush();
								}

								else if(fileRequested.contains(".gif")){

									FileReader fr = new FileReader("."+fileRequested);
									BufferedReader br = new BufferedReader(fr);
									String fileLine;
									dataOut.write(("GIF89a").getBytes());
									while ((fileLine = br.readLine()) != null) {
										dataOut.write((fileLine).getBytes());								
									}

									dataOut.flush();
									placeholderTest=true;
								}



							}else{

								out.print("HTTP/1.1 404 Not Found\r\n");  
								out.print("Content-Type: "+content+"\r\n");
								out.print("Content-Length: "+fileLength+"\r\n");
								out.print("Date: " +formatted+"\r\n"); 
								out.print("Connection: close\r\n"); 
								out.print("\r\n");
							}
						}

						else{
							out.print("HTTP/1.1 404 Not Found\r\n");  
							out.println("Date: " +formatted+"\r\n"); 
							out.print("Connection: close\r\n"); 
							out.print("\r\n\r\n");
						}
					}
				}

				if (method.equals("HEAD")) { 
					boolean isFileAvailable = checkAvailability(fileRequested);
					if(isFileAvailable){						
						out.print("HTTP/1.1 200 OK\r\n");  
						out.print("Content-Type: "+content+"\r\n");						
						out.print("\r\n");

					}else{
						out.print("HTTP/1.1 400 Not Found\r\n");  
						out.print("Content-Type: text/plain\r\n");						
						out.print("\r\n\r\n");
					}
				}

				if (method.equals("TRACE")) { 
					String fileNewName;
					File fileNow;
					System.out.println("Inside TRACE :"+content);
					if(fileRequested.indexOf('/') >= 0 && fileRequested.lastIndexOf('/') > 0 && (fileRequested.indexOf('/') != fileRequested.lastIndexOf('/'))){
						fileNewName = "."+fileRequested;
						fileNow = new File(fileNewName);
						fileLength = String.valueOf(fileNow.length());
					}else{

						fileNow = new File(fileRequested);
						fileLength = String.valueOf(fileNow.length());
					}
					fileRequested = fileRequested.replace("./public","");
					fileRequested = fileRequested.replace("index.html","");
					
					System.out.println("Here :"+fileRequested);
					out.print("HTTP/1.1 200 OK"+"\r\n");  
					out.print("Date: "+formatted+"\r\n");
					out.print("Server :localhost:8090"+"\r\n");
					out.print("Connection: close"+"\r\n");
					out.print("Content-Type: "+content);	
					out.print("\r\n\r\n");
					out.print("TRACE "+fileRequested+" HTTP/1.1"+"\r\n");  
					out.print("Host: "+host+"\r\n");	
					out.print("Connection: close"+"\r\n");
					out.print("\r\n");


				}

				if (method.equals("POST")) { 
					out.print("HTTP/1.1 501 Not Implemented\r\n"); 
					out.print("Content-Type: "+content+"\r\n");						
					out.print("\r\n");
				}

				if (method.equals("OPTIONS")) { 
					String allowedMethods = "";
					for(String method: allowList){
						allowedMethods = allowedMethods+method +",";
					}
					allowedMethods = allowedMethods.substring(0,allowedMethods.length()-1);

					out.print("HTTP/1.1 200 OK\r\n");  
					out.print("Allow: "+allowedMethods+"\r\n");									
					out.print("\r\n\r\n");
				}

				out.close();
				dataOut.close();

				in.close();  
				client.close();  
			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean checkAvailability(String fileName){

		System.out.println("file at Check :"+fileName);
		String directory = fileName.substring(0,fileName.lastIndexOf('/'));
		String filename = fileName.substring(fileName.lastIndexOf('/')+1);
		
		System.out.println("Dir :"+directory);
		System.out.println("File :"+filename);
		File currentDir = new File(directory);
		if(directory.equals("./public")){
			filename = "index.html";
		}
		return displayDirectoryContents(currentDir,filename);
	 
	}
	
 
	public static boolean displayDirectoryContents(File dir,String filename) {
		System.out.println("File2 :"+filename);
		 try{
			File[] files = dir.listFiles();
			System.out.println("LEN : "+files.length);
			for (File file : files) {
				System.out.println("FILE NAME :"+file.getName());
				if (file.isDirectory()) {				
					displayDirectoryContents(file,filename);
				} else {
					if(file.getName().equals(filename)){
						
						isFileAvailable = true;
						System.out.println("Right here"+isFileAvailable);
						break;
					}
				}
			}
		 }catch(NullPointerException ne){
			 isFileAvailable = false;
		 }
		System.out.println("Right here11"+isFileAvailable);
		return isFileAvailable;
	}

	private static String getContentType(String fileRequested,String method) {
		if(method.equals("TRACE")){
			return "message/http";
		}
		else if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
			return "text/html";
		else if(fileRequested.contains("directory3isempty")){
			return "application/octet-stream";
		}
		else if (fileRequested.endsWith(".xml")  ||  fileRequested.endsWith(".XML")){
			return "text/xml";
		}else if (fileRequested.endsWith(".jpeg")  ||  fileRequested.endsWith(".jpg")){
			return "image/jpeg";
		}
		else
			return "text/plain";
	}


	public static String getServerTime() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(calendar.getTime());
	}



	public static String getLastModified(String filePath) {
		File file = new File(filePath);
		Date lastModified = new Date(file.lastModified());
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(lastModified);
	}


	public static boolean verifyCaseSensitiveFiles(String fileDir){
		System.out.println("URL Encoded ---"+fileDir);
		String fileName = fileDir.substring(fileDir.lastIndexOf('/')+1);
		fileDir = fileDir.substring(0,fileDir.lastIndexOf('/'));
		File file = new File(fileDir);
		String[] files = file.list();
		for(String f : files){
			System.out.println("File "+f);
			if(f.equals(fileName))
				return true;
		}
		return false;

	}

	public static void sendResponse(String methodType) throws IOException{

		if(methodType == "GET"){
			dataOut.writeBytes("HTTP/1.1 200 OK");
			dataOut.writeBytes("Date: "+getServerTime());
			dataOut.writeBytes("Server: localhost");
			dataOut.writeBytes("Last-Modified: ");
			dataOut.writeBytes("Accept-Ranges: bytes");
			dataOut.writeBytes("Content-Length: ");
			dataOut.writeBytes("Connection: close");
			dataOut.writeBytes("Content-Type: text/html");
		}
	}
}
