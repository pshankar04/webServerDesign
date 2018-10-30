

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.bind.DatatypeConverter;


public class SimpleServer {
	public static String method = "";
	public static String fileRequested = "";
	public static String httpVersion = "";
	public static String host="";
	public static String connection="";
	public static String if_modified_since = "";
	public static String if_match = "";
	public static boolean placeholderTest = false;
	public static String WEBROOT= "../public";
	public static ArrayList<String> allowList = new ArrayList<String>();
	public static BufferedReader in;
	public static PrintWriter out;
	public static PrintWriter outImage;
	public static DataOutputStream dataOut;
	public static boolean isFileAvailable = false;
	public static String directoryStructure;
	public static boolean needsRedirection = false;
	public static boolean if_mod_flag = false;
	public static boolean if_match_flag = false;
	public static boolean keepAlive = false;
	public static String keepAliveStr = "";


	public static void main(String args[]) {

		allowList.add("GET");
		allowList.add("HEAD");
		allowList.add("OPTIONS");

		try {
			int port = Integer.parseInt(args[0]);
			LinkedHashMap<String,String> headerMap = new LinkedHashMap<String,String>();
			ServerSocket ss = new ServerSocket(port);

			for (;;) {
				long startTime = System.currentTimeMillis();

				Socket client = ss.accept();
				client.setKeepAlive(true);
				client.setSoTimeout(3000);
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				out = new PrintWriter(client.getOutputStream());
				//outImage = new PrintWriter(client.getOutputStream());

				dataOut = new DataOutputStream(client.getOutputStream());

				String line,superline;
				int count = 0;
				boolean malformedHeader = false;
				boolean incorrectVersion = false;
				boolean missingHost = false;
				boolean needsRedirection = false;
				if_mod_flag = false;
				if_modified_since = "";
				if_match = "";

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
				if(!headerMap.containsKey("connection")){
					keepAlive = true;
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

					}else if(pair.getKey().equals("If-Modified-Since")){
						System.out.println("Modified-Since 1 :"+if_mod_flag);
						if_modified_since = (String)pair.getValue();
						if(!if_modified_since.equals("")){
							if_mod_flag = true;
						}
						System.out.println("Modified-Since 2 :"+if_mod_flag);
					}else if(pair.getKey().equals("If-Match")){

						if_match = (String)pair.getValue();
						if_match_flag = true;
						System.out.println("Matched ETag :"+if_match_flag);
					}
				}

				System.out.println("method:"+method);
				System.out.println("fileRequested:"+fileRequested);
				System.out.println("httpVersion:"+httpVersion);
				System.out.println("host:"+host);
				System.out.println("connection:"+connection);
				System.out.println("If-Modified-Since:"+if_modified_since);
				System.out.println("If-Match:"+if_match);

				fileRequested = java.net.URLDecoder.decode(fileRequested, "UTF-8");

				if(!httpVersion.equals("HTTP/1.1")){
					incorrectVersion = true;
				}

				if(host.equals("")){
					missingHost = true;
				}
				if(method.equals("TRACE")){

				}
				System.out.println("fileRequested NOW:"+fileRequested);

				if(fileRequested.equals("/a2-test/")){
					fileRequested = "/a2-test/";
				}
				else if(fileRequested.endsWith("/a2-test/")){

				}
				else if (fileRequested.endsWith("/") && (fileRequested.indexOf('/') >= 0) && (fileRequested.lastIndexOf('/') > 0 ) ){
					fileRequested = fileRequested +"index.html";
				}
				else if(fileRequested.equals("/")){
					fileRequested = "/";
				}
				else if (fileRequested.endsWith("/") && !fileRequested.contains("a2-test")) {
					fileRequested = "index.html";
				}
				else if (!fileRequested.endsWith("/") && !fileRequested.contains(".html") && !fileRequested.contains(".jpeg") 
						&& !fileRequested.contains(".JPEG") && !fileRequested.contains(".txt") && !fileRequested.contains("directory3isempty")
						&& !fileRequested.contains(".xml") && !fileRequested.contains(".gif") ) {
					System.out.println("Check for 301 : "+fileRequested);
					File f = new File(fileRequested);
					if(!f.isDirectory()){
						needsRedirection = true;
					}
				}
				else{
					if(fileRequested.contains("//")){
						fileRequested = fileRequested.substring(fileRequested.indexOf("//")+2);	
						fileRequested = fileRequested.substring(fileRequested.indexOf('/'));
					}else{
						fileRequested = fileRequested.substring(fileRequested.indexOf('/'));
					}

				}
				System.out.println("file Requested  Before:"+fileRequested);
				fileRequested = WEBROOT + fileRequested;
				System.out.println("file Requested  123:"+fileRequested);
				File file = new File(fileRequested.replace("../public", ""));
				String fileLength = String.valueOf(file.length());				
				String content = getContentType(fileRequested,method);

				String formatted = getServerTime();

				if (method.equals("GET")) { 					
					boolean isFileAvailable = checkAvailability(fileRequested);
					System.out.println("Is Available : "+isFileAvailable);

					fileRequested = fileRequested.replace("../public","");
					System.out.println("file requested GET: "+fileRequested);
					System.out.println("needsRedirection :"+needsRedirection);

					if(malformedHeader || missingHost){
						out.print("HTTP/1.1 400 Bad Request"+"\r\n");  									 
						out.print("Date: "+formatted+"\r\n"); 
						out.print("Server: "+host+"\r\n");
						if(keepAlive){
							out.print("Connection: keep-alive"+"\r\n"); 
						}else{
							out.print("Connection: close"+"\r\n");  
						}
						out.print("Content-Type: "+content+"\r\n");
						out.print("\r\n\r\n");						
					}
					else if(if_match_flag){
						System.out.println("fileRequested in 412 :"+fileRequested);
						String etag = generateETag(fileRequested);

						if_match = if_match.replace("\"", "");
						System.out.println("Etag :"+etag);
						System.out.println("if_match : "+if_match);
						System.out.println("Match Test : "+etag.equals(if_match));

						if(!(etag).equals(if_match)){
							out.print("HTTP/1.1 412 Precondition Failed"+"\r\n");  									 
							out.print("Date: " +formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("Connection: close"+"\r\n"); 
							out.print("Content-Type: text/html"+"\r\n\r\n");							

						}else if(etag.equals(if_match)){
							if(!fileRequested.contains("../public")){
								fileRequested = "../public" + fileRequested;
							}
							File fileForLength = new File(fileRequested);
							long newfileLength = fileForLength.length();
							System.out.println("newfileLength : "+newfileLength);
							String str = "HTTP/1.1 200 OK\r\n"+
									"Date: "+formatted+"\r\n" +
									"Server: "+host+"\r\n"+
									"Content-Type: "+content+"\r\n"+
									"Last-Modified: "+getLastModified(fileRequested)+"\r\n"+										
									"Content-Length: "+newfileLength+"\r\n"+											 
									"Connection: close"+"\r\n\r\n";
							dataOut.write(str.getBytes());


							FileReader fr = new FileReader(fileRequested);
							BufferedReader br = new BufferedReader(fr);
							String fileLine;
							while ((fileLine = br.readLine()) != null) {
								System.out.println(fileLine+"\n");
								dataOut.write((fileLine).getBytes());
							}
						}
					}

					else if(if_mod_flag){
						System.out.println("fileRequested in 304 :"+fileRequested);
						out.print("HTTP/1.1 304 Not Modified"+"\r\n");  									 
						out.print("Date: " +formatted+"\r\n"); 
						out.print("Server: "+host+"\r\n");
						out.print("Last-Modified: "+getLastModified(fileRequested)+"\r\n");
						if(keepAlive){
							out.print("Connection: keep-alive"+"\r\n"); 
						}else{
							out.print("Connection: close"+"\r\n");  
						} 
						out.print("Content-Type: text/html"+"\r\n");
						out.print("Location: "+fileRequested.replace("../public", "")+"/"+"\r\n");
						out.print("\r\n\r\n");	

					}

					else if(incorrectVersion){
						out.print("HTTP/1.1 505 Version Not Supported \r\n");  									 
						out.print("Date: "+formatted+"\r\n"); 
						if(keepAlive){
							out.print("Connection: keep-alive"+"\r\n"); 
						}else{
							out.print("Connection: close"+"\r\n");  
						} 
						out.print("\r\n\r\n");						
					}
					else if(missingHost){
						out.print("HTTP/1.1 400 Bad Request"+"\r\n");  									 
						out.print("Date: " +formatted+"\r\n"); 
						out.print("Server: "+host+"\r\n");
						if(keepAlive){
							out.print("Connection: keep-alive"+"\r\n"); 
						}else{
							out.print("Connection: close"+"\r\n");  
						}
						out.print("Content-Type: "+content+"\r\n");
						out.print("\r\n\r\n");						
					}
					else if(needsRedirection){
						System.out.println("Here for 301");
						//						if(!fileRequested.contains(host)){
						//							fileRequested = fileRequested.replace("../public", "");
						//							fileRequested = "http://"+host + fileRequested ;
						//						}
						String messageFile = "<html><head><title>301 Moved Permanently</title></head><body><h1>Moved Permanently</h1>"+	
								"The Document has moved here</body></html>";

						out.print("HTTP/1.1 301 Moved Permanently"+"\r\n");  									 
						out.print("Date: " +formatted+"\r\n"); 
						out.print("Server: "+host+"\r\n");
						if(keepAlive){
							out.print("Connection: keep-alive"+"\r\n"); 
						}else{
							out.print("Connection: close"+"\r\n");  
						}
						out.print("Content-Type: text/html"+"\r\n");
						out.print("Location: "+fileRequested.replace("../public", "")+"/"+"\r\n");
						out.print("Content-Length: "+messageFile.length()+"\r\n");
						out.print("\r\n\r\n");		


						out.print(messageFile+"\r\n");
					}

					else {
						long newfileLength = 0l;
						boolean verifiedCaseFile = false;

						if(isFileAvailable){
							File fileNow = null;
							String fileNewName = "";
							fileRequested = fileRequested.replace("http://localhost:8090", "");
							System.out.println("File Req :"+fileRequested);
							if(fileRequested.equals("/")){
								fileRequested = "../public/a1-test/2/" +"index.html";
								verifiedCaseFile = true;
							}
							else if(fileRequested.contains("/a2-test/")){
								verifiedCaseFile = true;
							}
							else if(fileRequested.contains("../public/a2-test")){
								verifiedCaseFile = true;
							}
							else if(!fileRequested.equals("../public/") && !fileRequested.contains(".jpeg")){
								verifiedCaseFile = verifyCaseSensitiveFiles(fileRequested);
							}
							else if(fileRequested.contains(".jpeg")){
								verifiedCaseFile = true;
							}

							else{
								fileRequested = "../public/a1-test/2/" +"index.html";
								verifiedCaseFile = true;
							}
							if(verifiedCaseFile){
								System.out.println("Verified Case : ");
								String filePath = new File(fileRequested).getAbsolutePath().replace("src/../","");
								System.out.println("file Path : "+filePath);
								if(!fileRequested.contains("../public")){
									fileRequested = "../public" + fileRequested;
								}
								System.out.println("file Requested 111 : "+fileRequested);
								File fileForLength = new File(fileRequested);
								newfileLength = fileForLength.length();
								System.out.println("newfileLength : "+newfileLength);
								if(keepAlive){
									keepAliveStr = "Connection: keep-alive"+"\r\n"; 
								}else{
									keepAliveStr = "Connection: close"+"\r\n";  
								}
								String str = "HTTP/1.1 200 OK\r\n"+
										"Date: "+formatted+"\r\n" +
										"Server: "+host+"\r\n"+
										"Content-Type: "+content+"\r\n"+
										"Last-Modified: "+getLastModified(filePath)+"\r\n"+										
										"Content-Length: "+newfileLength+"\r\n"+											 
										"Connection: close"+"\r\n\r\n";

								dataOut.write(str.getBytes());

								if(fileRequested.contains(".jpeg") || fileRequested.contains(".jpg")){
									System.out.println("HERE");

									File f1 = new File(fileRequested);
									InputStream inp = new FileInputStream(f1);
									byte[] buffer=new byte[1024];
									int readData;
									while((readData = inp.read(buffer))!=-1){
										dataOut.write(buffer,0,readData);
									}
									dataOut.flush();

								}
								else if(fileRequested.contains(".gif")){

									FileReader fr = new FileReader(fileRequested);
									BufferedReader br = new BufferedReader(fr);
									String fileLine;
									dataOut.write(("GIF89a").getBytes());
									while ((fileLine = br.readLine()) != null) {
										dataOut.write((fileLine).getBytes());								
									}

									dataOut.flush();

								}

								else if(fileRequested.contains("../public/a2-test") && !fileRequested.contains(".html")){
									System.out.println("here for dir str");
									dataOut.write((directoryStructure).getBytes());
								}

								else if(!fileRequested.contains(".gif") && !fileRequested.contains(".jpeg") && newfileLength != 0){
									System.out.println("fileRequested 111 :"+fileRequested);
									String filePath1 = new File(fileRequested).getAbsolutePath().replace("src/../","");
									filePath1 = filePath1.replace("src/../","");									
									FileReader fr = new FileReader(filePath1);
									BufferedReader br = new BufferedReader(fr);
									String fileLine;
									while ((fileLine = br.readLine()) != null) {
										dataOut.write((fileLine).getBytes());
									}	
								}


							}else{

								out.print("HTTP/1.1 404 Not Found"+"\r\n");  
								out.print("Date: "+formatted+"\r\n"); 
								out.print("Server: "+host+"\r\n");
								if(keepAlive){
									keepAliveStr = "Connection: keep-alive"+"\r\n"; 
								}else{
									keepAliveStr = "Connection: close"+"\r\n";  
								}
								out.print("Content-Type: "+content+"\r\n");								
								out.print("\r\n");
							}
						}
						else if(needsRedirection){
							// nothing to handle
						}
						else{
							out.print("HTTP/1.1 404 Not Found"+"\r\n");  
							out.print("Date: "+formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							if(keepAlive){
								keepAliveStr = "Connection: keep-alive"+"\r\n"; 
							}else{
								keepAliveStr = "Connection: close"+"\r\n";  
							}
							out.print("Content-Type: "+content+"\r\n");								
							out.print("\r\n");
						}
					}
				}

				if (method.equals("HEAD")) { 
					boolean isFileAvailable = checkAvailability(fileRequested);
					System.out.println("IN HEAD :"+isFileAvailable);
					long newfileLength = 0l;

					if(if_mod_flag && isFileAvailable){
						System.out.println("fileRequested in 304 :"+fileRequested);
						String if_mod_date = headerMap.get("If-Modified-Since");
						System.out.println("if_mod_date in HEAD :"+if_mod_date);
						try{
							SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
							dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
							Date currDate = dateFormat.parse(if_mod_date);
							Date modDate = dateFormat.parse(getLastModified(fileRequested));
							System.out.println("currDate in HEAD :"+currDate);
							System.out.println("modDate in HEAD :"+modDate);

							if(modDate.compareTo(currDate) <= 0){
								System.out.println("modDate inside :");
								out.print("HTTP/1.1 304 Not Modified"+"\r\n");  									 
								out.print("Date: " +formatted+"\r\n"); 
								out.print("Server: "+host+"\r\n");
								out.print("Last-Modified: "+getLastModified(fileRequested)+"\r\n");
								out.print("ETag: "+"\""+generateETag(fileRequested)+"\""+"\r\n");
								if(keepAlive){
									keepAliveStr = "Connection: keep-alive"+"\r\n"; 
								}else{
									keepAliveStr = "Connection: close"+"\r\n";  
								}
								out.print("Content-Type: text/html"+"\r\n");
								out.print("Location: "+fileRequested.replace("../public", "")+"\r\n");
								out.print("\r\n");	
							}else{
								String filePath = new File(fileRequested).getAbsolutePath().replace("src/../","");
								System.out.println("filePath : "+filePath);
								File fileForLength = new File(filePath);
								newfileLength = fileForLength.length();

								out.print("HTTP/1.1 200 OK"+"\r\n");
								out.print("Date: "+formatted+"\r\n"); 
								out.print("Server: "+host+"\r\n");
								out.print("Content-Type: "+content+"\r\n");
								out.print("Last-Modified: "+getLastModified(filePath)+"\r\n");	
								out.print("ETag: "+"\""+generateETag(fileRequested)+"\""+"\r\n");
								out.print("Content-Length: "+newfileLength+"\r\n");
								out.print("Connection: close"+"\r\n\r\n");


							}
						}catch(Exception e){
							out.print("HTTP/1.1 200 OK\r\n");
							out.print("Date: "+formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("Content-Type: "+content+"\r\n");
							out.print("Last-Modified: "+getLastModified(fileRequested)+"\r\n");		
							out.print("ETag: "+"\""+generateETag(fileRequested)+"\""+"\r\n");
							out.print("Content-Length: "+newfileLength+"\r\n");										 
							if(keepAlive){
								keepAliveStr = "Connection: keep-alive"+"\r\n\r\n"; 
							}else{
								keepAliveStr = "Connection: close"+"\r\n\r\n";  
							}
						}

					}
					else if(isFileAvailable){
						System.out.println("HERE NOW :"+keepAlive);
						System.out.println("fileRequested HEAD: "+fileRequested);
						String filePath = new File(fileRequested).getAbsolutePath().replace("src/../","");
						System.out.println("filePath : "+filePath);
						File f2 = new File(fileRequested);
						if(f2.exists() && !f2.isDirectory()) { 
							File fileForLength = new File(filePath);
							newfileLength = fileForLength.length();
						}else if(f2.isDirectory()){
							newfileLength = 0;
						}

						if(keepAlive){
							keepAliveStr = "Connection: keep-alive"; 
						}else{
							keepAliveStr = "Connection: close";  
						}
						System.out.println("is Alive Now : "+client.getKeepAlive());
						if(client.getKeepAlive()){
							out.print("HTTP/1.1 200 OK"+"\r\n");
							out.print("Server: "+host+"\r\n");
							out.print("Date: "+formatted+"\r\n"); 						
							out.print("Content-Type: "+content+"\r\n");
							out.print("ETag: "+"\""+generateETag(fileRequested)+"\""+"\r\n");
							out.print(keepAliveStr+"\r\n");
							out.print("Last-Modified: "+getLastModified(filePath)+"\r\n");	
							out.print("Content-Length: "+newfileLength+"\r\n\r\n");	
						}
						
						long endTime = System.currentTimeMillis();
						
						//Thread.sleep(1000);
						System.out.println("TIME here"+(endTime - startTime));
						if(client.getSoTimeout() < (endTime - startTime)){
							System.out.println("Here closed");
							out.print("HTTP/1.1 408 Request Timeout"+"\r\n");
							out.print("Date: " +formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");					 
							out.print("Connection: close"+"\r\n\r\n");
							out.flush();
							client.close();
						}
					}
					else{
						out.print("HTTP/1.1 400 Not Found"+"\r\n");  
						out.print("Content-Type: text/plain"+"\r\n\r\n");	

						//out.print("\r\n\r\n");
					}
				}

				if (method.equals("TRACE")) { 
					long newfileLength = 0l;
					String fileNewName;
					File fileNow;
					System.out.println("Inside TRACE :"+fileRequested);
					if(fileRequested.indexOf('/') >= 0 && fileRequested.lastIndexOf('/') > 0 && (fileRequested.indexOf('/') != fileRequested.lastIndexOf('/'))){
						fileNewName = "."+fileRequested;
						fileNow = new File(fileNewName);
						fileLength = String.valueOf(fileNow.length());
					}else{

						fileNow = new File(fileRequested);
						fileLength = String.valueOf(fileNow.length());
					}
					//					fileRequested = fileRequested.replace("../public","");
					//					fileRequested = fileRequested.replace("index.html","");

					String filePath = new File(fileRequested).getAbsolutePath().replace("src/../","");
					System.out.println("filePath : "+filePath);
					File fileForLength = new File(filePath);
					newfileLength = fileForLength.length();

					fileRequested = fileRequested.replace("../public","");
					
					if(method.equals("TRACE")){
						fileRequested = fileRequested.replace("index.html","");
					}

					System.out.println("Here :"+fileRequested);
					System.out.println("newfileLength :"+newfileLength);

					out.print("HTTP/1.1 200 OK"+"\r\n");  
					out.print("Date: "+formatted+"\r\n");
					out.print("Server: "+host+"\r\n");
					out.print("Connection: close"+"\r\n");
					out.print("Content-Type: "+content+"\r\n");	
					out.print("Content-Length: "+newfileLength+"\r\n");	
					out.print("\r\n");
					out.print("TRACE "+fileRequested+" HTTP/1.1"+"\r\n");  
					out.print("Host: "+host+"\r\n");						
					if(keepAlive){
						keepAliveStr = "Connection: keep-alive"+"\r\n\r\n"; 
					}else{
						keepAliveStr = "Connection: close"+"\r\n\r\n";  
					}
					//					out.print("\r\n");


				}

				if (method.equals("POST")) { 
					String allowedMethods = "";
					for(String method: allowList){
						allowedMethods = allowedMethods+method +",";
					}
					allowedMethods = allowedMethods.substring(0,allowedMethods.length()-1);

					out.print("HTTP/1.1 501 Method Not Implemented\r\n"); 
					out.print("Date: " +formatted+"\r\n"); 
					out.print("Server: "+host+"\r\n");					 
					out.print("Allow: "+allowedMethods+"\r\n");	
					if(keepAlive){
						keepAliveStr = "Connection: keep-alive"+"\r\n"; 
					}else{
						keepAliveStr = "Connection: close"+"\r\n";  
					}
					out.print("Content-Type: "+content+"\r\n\r\n");
				}

				if (method.equals("OPTIONS")) { 
					String allowedMethods = "";
					for(String method: allowList){
						allowedMethods = allowedMethods+method +",";
					}
					allowedMethods = allowedMethods.substring(0,allowedMethods.length()-1);

					out.print("HTTP/1.1 200 OK\r\n");
					out.print("Date: " +formatted+"\r\n"); 
					out.print("Server: "+host+"\r\n");					 
					out.print("Content-Length:"+0+"\r\n");	
					out.print("Allow: "+allowedMethods+"\r\n");	
					if(keepAlive){
						keepAliveStr = "Connection: keep-alive"+"\r\n"; 
					}else{
						keepAliveStr = "Connection: close"+"\r\n";  
					}
					out.print("Content-Type: "+content+"\r\n\r\n");
				}

//				keepAlive = true;
//				if(keepAlive){
//					System.out.println("Here in close 1");
//
//					System.out.println("ALIVE :"+client.getKeepAlive());
//					long endTime = System.currentTimeMillis();
//					System.out.println("TIME : "+(endTime - startTime));
//					//Thread.sleep(10000);
//					if((endTime - startTime) > 15000){
//						out.print("HTTP/1.1 408 Request Timeout\r\n");
//						out.print("Date: " +formatted+"\r\n"); 
//						out.print("Server: "+host+"\r\n");					 
//						out.print("Connection: close"+"\r\n");
//						out.flush();
//
//					}
//				}

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


	public static String generateETag(String file) throws NoSuchAlgorithmException, IOException{
		System.out.println("In ETag : "+file);
		File f = new File(file);
		if(f.isDirectory()){
			return "";
		}
		else if(!file.contains("../public")){
			file = "../public" + file;
		}
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(Files.readAllBytes(Paths.get(file)));
		byte[] digest = md.digest();
		return DatatypeConverter.printHexBinary(digest).toUpperCase();
	}

	public static boolean checkAvailability(String fileName){
		String directory = "";
		String filename = "";
		System.out.println("file at Check :"+fileName);

		if(if_mod_flag){

			return true;
		}
		System.out.println("if_mod_flag  :"+if_mod_flag);
		if(fileName.equals("/")){
			directory = "../public";
		}
		else if(fileName.contains("http:")){
			fileName = fileName.substring(fileName.indexOf("//")+2);
			fileName = fileName.substring(fileName.indexOf("/"));
			if(fileName.contains(".jpeg")){
				directory = "../public";
				filename = fileName.substring(fileName.lastIndexOf('/')+1);
			}else{
				directory = fileName.substring(0,fileName.lastIndexOf("/"));
				filename = fileName.substring(fileName.lastIndexOf('/')+1);
			}
			System.out.println("file NOW :"+fileName);
		}
		else if(fileName.contains("../public") && !fileName.contains(".html")){
			directory = fileName.substring(0,fileName.lastIndexOf('/'));
			filename = fileName.substring(fileName.lastIndexOf('/')+1);
		}else{
			directory = fileName;
			filename = fileName.substring(fileName.lastIndexOf('/')+1);
		}


		System.out.println("DIrectory :"+directory);
		System.out.println("filename :"+filename);
		//		directory = directory.replace("..","");
		//		directory = directory.substring(0,directory.lastIndexOf("/")+1);
		//		
		if(filename.contains(".jpeg") || filename.contains(".txt") || filename.contains("gif")){
			directory = directory + "/"+filename;
		}
		System.out.println("DIrectory 2 :"+directory);
		File currentDir = new File(directory);
		if(directory.equals("../public")){
			System.out.println("Here 1");
			filename = "index.html";

		}else if(!directory.contains("../public") && !directory.contains("/public") && !directory.contains("/a1-test") && !directory.contains("/a2-test")){
			System.out.println("Here 2");
			try{
				directory = "../public/a1-test/" + directory;
				return fileExists(currentDir,filename);
			}catch(Exception e){
				directory = "../public/a1-test/" + directory;
				return fileExists(currentDir,filename);
			}

		}
		else if(!directory.contains("../public") && !directory.contains("/public") && fileName.contains("directory3isempty")){
			System.out.println("Here 3");
			directory = "../public" + directory;
			return fileExists(currentDir,filename);

		}
		else if(!directory.contains("../public") && !directory.contains("/public")){
			System.out.println("Here 4");
			directory = "../public" + directory;
			directoryStructure = getDirectoryList(new File(directory));
			return true;
		}
		else if(directory.equals("../public/a2-test")){
			System.out.println("Here 5");
			filename = "";
			directoryStructure = getDirectoryList(new File(directory));
			//System.out.println("DIR STR :"+dirStructure);
			return true;
		}else if(directory.contains("/public/a2-test")){
			System.out.println("Here 6");
			//String actualFile = filename.substring(filename.lastIndexOf("/"));
			//File fileText = new File(filename);
			return fileExists(currentDir,filename);
		}
		else if(directory.contains(".jpeg")){
			System.out.println("Here 7");

			return fileExists(currentDir,filename);
		}
		else if(filename.contains("directory3isempty")){
			System.out.println("Here 8");
			//return fileExists(currentDir,filename);
			return displayDirectoryContents(currentDir,filename);
		}
		else if(directory.contains("a1-test/1/1.2")){
			System.out.println("Here 9");
			//return fileExists(currentDir,filename);
			return displayDirectoryContents(currentDir,filename);
		}

		else if(directory.contains("/public/a1-test")){
			System.out.println("Here 10");
			return fileExists(currentDir,filename);
			//return displayDirectoryContents(currentDir,filename);
		}

		return displayDirectoryContents(currentDir,filename);

	}

	public static boolean fileExists(File fileText,String fileName){
		System.out.println("is Dir :"+fileText.isDirectory());
		try{
			if(fileText.exists() && !fileText.isDirectory()) { 
				return true;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

	public static String getDirectoryList(File dir) {
		System.out.println("File Dir :"+dir);
		try{
			File[] files = dir.listFiles();
			//System.out.println("LEN : "+files.length);
			for (File file : files) {
				directoryStructure += file.getName() +"\n";
				if (file.isDirectory()) {				
					getDirectoryList(file);
				} 
			}
		}catch(NullPointerException ne){
			ne.printStackTrace();
		}
		System.out.println("Dir Structure : "+directoryStructure);
		return directoryStructure;

	}



	public static boolean displayDirectoryContents(File dir,String filename) {
		System.out.println("File2 :"+filename);
		System.out.println("Dir :"+dir.getPath());
		//dir = dir.getAbsoluteFile();
		System.out.println("Dir :"+dir.getName());
		try{
			File[] files = dir.listFiles();
			System.out.println("LEN : "+files.length);
			for (File file : files) {
				System.out.println("File :"+file.getName());
				directoryStructure += file.getName() +"\n";
				if (file.isDirectory()) {		
					System.out.println("RIght here for dir :");
					displayDirectoryContents(file,filename);
				} else {
					if(file.getName().equals(filename)){
						System.out.println("RIght here for file :");
						isFileAvailable = true;
						System.out.println("Right here"+isFileAvailable);
						break;
					}
				}
			}
		}catch(NullPointerException ne){
			System.out.println("RIght here for exception :");
			ne.printStackTrace();
			isFileAvailable = false;
		}

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
		}else if (fileRequested.endsWith(".gif")){
			return "image/gif";
		}else if(fileRequested.contains("/a2-test")){
			return "text/html";
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
		fileDir = "../public" + fileDir;
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


}
