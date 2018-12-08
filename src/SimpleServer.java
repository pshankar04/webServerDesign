import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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
	public static String setString = "";
	public static String reqSet = "iso-2022-jp";
	public static LinkedHashMap<String,String> headerMap;
	public static int lengthForPartialContent = 0;
	public static int totalLengthForPartialContent = 0;


	public static void main(String args[]) throws SocketTimeoutException{

		allowList.add("GET");
		allowList.add("HEAD");
		allowList.add("OPTIONS");

		try {
			int port = Integer.parseInt(args[0]);
			//			LinkedHashMap<String,String> headerMap;
			ServerSocket ss = new ServerSocket(port);
			long lastRequestTime = 0l;
			long requestReceivedTime = 0l;
			int testCount = 0;

			for (;;) {
				Socket client = ss.accept();
				headerMap = new LinkedHashMap<String,String>();
				requestReceivedTime = System.currentTimeMillis();
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				out = new PrintWriter(client.getOutputStream());
				dataOut = new DataOutputStream(client.getOutputStream());
				System.out.println("TEST COUNT:"+testCount);
				if(testCount > 0){
					System.out.println("TIME EXEC : "+(requestReceivedTime - lastRequestTime));
					if((requestReceivedTime - lastRequestTime) > 15000){

						client.setKeepAlive(false);
						//client.close();
					}else{
						lastRequestTime = requestReceivedTime;
						client.setSoTimeout(15000);
						client.setKeepAlive(true);
					}
				}
				testCount++;

				String line,superline;
				int count = 0;
				boolean malformedHeader = false;
				boolean incorrectVersion = false;
				boolean missingHost = false;
				boolean needsRedirection = false;
				boolean isTransferEncoding = false;
				boolean noAcceptHeader = false;
				boolean acceptHeader = false;
				boolean zeroQValue = false;
				boolean acceptCharset = false;
				boolean send406 = false;
				boolean needsAuthentication = false;
				if_mod_flag = false;
				if_modified_since = "";
				if_match = "";
				if_match_flag = false;

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

				Iterator<Map.Entry<String, String>> it1 = headerMap.entrySet().iterator();
				while (it1.hasNext()) {
					Map.Entry<String,String> pair = (Map.Entry<String,String>)it1.next();
					System.out.println(pair.getKey()+" : "+pair.getValue());
				}

				if(!headerMap.containsKey("connection") || !headerMap.containsKey("Connection")){
					System.out.println("Here for alive");
					keepAlive = true;
					//					client.setSoTimeout(15000);
					//					client.setKeepAlive(true);

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
						System.out.println("if_modified_since :"+if_modified_since);
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
				}else if(!headerMap.containsKey("Accept") && fileRequested.endsWith("fairlane")){
					System.out.println("noAcceptHeader here");
					noAcceptHeader = true;
				}else if(headerMap.containsKey("Accept") && fileRequested.endsWith("fairlane") && headerMap.get("Accept").contains("*") && 
						headerMap.get("Accept").contains("q=1.0") ){
					System.out.println("AcceptHeader here"+headerMap.get("Accept"));
					acceptHeader = true;
				}else if(headerMap.containsKey("Accept-Language")){
					System.out.println("AcceptHeader language"+headerMap.get("Accept-Language"));
					acceptHeader = true;
				}else if(headerMap.containsKey("Accept-Language") && headerMap.containsKey("Accept-Charset")){
					System.out.println("AcceptHeader language"+headerMap.get("Accept-Language"));
					acceptHeader = true;
					acceptCharset = true;
				}
				else if(fileRequested.contains("protected")){
					needsAuthentication = true;
				}
				else if (!fileRequested.endsWith("/") && !fileRequested.contains(".html") && !fileRequested.contains(".jpeg") 
						&& !fileRequested.contains(".JPEG") && !fileRequested.contains(".txt") && !fileRequested.contains("directory3isempty")
						&& !fileRequested.contains(".xml") && !fileRequested.contains(".gif") ) {
					System.out.println("Check for 301 : "+fileRequested);
					File f = new File(fileRequested);
					if(!f.isDirectory()){
						needsRedirection = true;
					}
				}else if(fileRequested.endsWith("index.htmll")){
					isTransferEncoding = true;
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
							File fileNow = new File(fileRequested);
							long newfileLength = fileNow.length();
							out.print("HTTP/1.1 412 Precondition Failed"+"\r\n");  									 
							out.print("Date: " +formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("Transfer-Encoding: chunked"+"\r\n");
							out.print("Content-Length: "+newfileLength+"\r\n");
							out.print("Content-Type: text/html"+"\r\n");
							out.print("Connection: close"+"\r\n\r\n"); 
							out.print(getChunkedBytes("../public/a3-test/412.html"));
							out.print("\r\n\r\n");


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
									"Content-Language: "+getContentLanguage(fileRequested)+"\r\n"+
									"Content-Length: "+newfileLength+"\r\n"+											 
									"Connection: close"+"\r\n\r\n";
							dataOut.write(str.getBytes());

							if(fileRequested.endsWith("ru.koi8-r")){
								File f1 = new File(fileRequested);
								InputStream inp = new FileInputStream(f1);
								byte[] buffer=new byte[1024];
								int readData;
								while((readData = inp.read(buffer))!=-1){
									dataOut.write(buffer,0,readData);
								}
								dataOut.flush();
							}else{
								FileReader fr = new FileReader(fileRequested);
								BufferedReader br = new BufferedReader(fr);
								String fileLine;
								while ((fileLine = br.readLine()) != null) {
									System.out.println(fileLine+"\n");
									dataOut.write((fileLine).getBytes());
								}
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
					}else if(noAcceptHeader){
						long newfileLength = 0l;
						File fileForLength = new File(fileRequested+".txt");
						newfileLength = fileForLength.length();
						out.print("HTTP/1.1 300 Multiple Choice"+"\r\n");  									 
						out.print("Date: " +formatted+"\r\n"); 
						out.print("Server: "+host+"\r\n");
						out.print("Content-Length: "+newfileLength+"\r\n");
						out.print("Content-Type: "+getContentType(fileRequested+".html","GET")+"\r\n");
						out.print("Connection: close"+"\r\n"); 
						out.print("\r\n\r\n");	

					}else if(needsAuthentication){
						System.out.println(" Authorization file ;"+fileRequested);
						fileRequested = fileRequested.replace("http://localhost:8090/", "../public/");
						File fileForLength = new File(fileRequested);
						long newfileLength = fileForLength.length();
						System.out.println(" Authorization Length ;"+newfileLength);
						if(headerMap.containsKey("Authorization") && confirmAuthorization(fileRequested,headerMap.get("Authorization"))){
							out.print("HTTP/1.1 200 OK"+"\r\n");  									 
							out.print("Date: " +formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("Content-Type: "+getContentType(fileRequested,"GET")+"; charset=iso-8859-1"+"\r\n");
							out.print("Content-Length: "+newfileLength+"\r\n");
							out.print("Connection: close"+"\r\n"); 
							out.print("\r\n\r\n");
							
							out.print("<html><body>this file is protected</body></html>"+"\r\n\r\n");
						}else{
							out.print("HTTP/1.1 401 Authorization Required"+"\r\n");  									 
							out.print("Date: " +formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("WWW-Authenticate: Basic realm=\"Fried Twice\""+"\r\n");
							out.print("Content-Type: "+getContentType(fileRequested,"GET")+"; charset=iso-8859-1"+"\r\n");
							out.print("Connection: close"+"\r\n"); 
							out.print("\r\n\r\n");	
						}
					}
					else if(acceptHeader){
						long newfileLength = 0l;
						System.out.println("Here in accept header");
						if(fileRequested.endsWith("fairlane")){
							File fileForLength = new File(fileRequested+".txt");
							newfileLength = fileForLength.length();
							out.print("HTTP/1.1 300 Multiple Choice"+"\r\n");  									 
							out.print("Date: " +formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("Content-Length: "+newfileLength+"\r\n");
							out.print("Content-Type: "+getContentType(fileRequested+".html","GET")+"\r\n");
							out.print("Transfer-Encoding: chunked"+"\r\n");
							out.print("Connection: close"+"\r\n"); 
							out.print("\r\n");	
							out.print(getChunkedBytes("../public/a3-test/fairlane.html"));
							out.print("\r\n\r\n");
						}else{
							File fileForLength = new File(fileRequested+".en");
							newfileLength = fileForLength.length();
							out.print("HTTP/1.1 300 Multiple Choice"+"\r\n");  									 
							out.print("Date: " +formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("Content-Length: "+newfileLength+"\r\n");
							out.print("Content-Type: "+getContentType(fileRequested+".html","GET")+"\r\n");
							out.print("Transfer-Encoding: chunked"+"\r\n");
							out.print("Connection: close"+"\r\n"); 
							out.print("\r\n");	
							out.print(getChunkedBytes("../public/a3-test/index.html.en"));
							out.print("\r\n\r\n");
						}
					}

					else {
						long newfileLength = 0l;
						boolean verifiedCaseFile = false;
						if(headerMap.containsKey("Range")){
							verifiedCaseFile = true;
						}
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
								System.out.println("Verified Case...");
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

								if(headerMap.containsKey("Range")){
									System.out.println("fileRequested :"+fileRequested);
									File fname = new File(fileRequested);
									lengthForPartialContent = (int) fname.length();
									String rangeStr = headerMap.get("Range");
									rangeStr = rangeStr.substring(rangeStr.indexOf("=")+1, rangeStr.length());
									System.out.println("rangeStr :"+rangeStr);
									int initialIndex = Integer.parseInt(rangeStr.substring(0,rangeStr.indexOf('-')));
									int finalIndex = Integer.parseInt(rangeStr.substring(rangeStr.indexOf('-') + 1));
									System.out.println("INDEXXXXXXX :"+initialIndex+"   "+finalIndex);
									totalLengthForPartialContent = finalIndex - initialIndex + 1;
									newfileLength = Integer.parseInt(rangeStr.substring(rangeStr.indexOf('-')+1)) + 1;

									String str = "HTTP/1.1 206 Partial Content"+"\r\n"+
											"Date: "+formatted+"\r\n" +
											"Server: "+host+"\r\n"+
											"Content-Type: "+content+"\r\n"+
											"Last-Modified: "+getLastModified(filePath)+"\r\n"+	
											"ETag: "+"\""+generateETag(fileRequested)+"\""+"\r\n"+
											"Accept-Ranges: bytes"+"\r\n"+
											"Content-Length: "+totalLengthForPartialContent+"\r\n"+
											"Content-Language: "+getContentLanguage(fileRequested)+"\r\n"+
											"Content-Range: bytes "+rangeStr+"/"+lengthForPartialContent+"\r\n\r\n";
									dataOut.write(str.getBytes());

								}else{
									System.out.println("Right here...."+filePath);

									String str = "HTTP/1.1 200 OK\r\n"+
											"Date: "+formatted+"\r\n" +
											"Server: "+host+"\r\n"+
											"ETag: "+"\""+generateETag(fileRequested)+"\""+"\r\n"+
											"Content-Type: "+getContentType(fileRequested, "GET")+"\r\n"+
											"Content-Language: "+getContentLanguage(fileRequested)+"\r\n"+
											"Last-Modified: "+getLastModified(filePath)+"\r\n"+										
											"Content-Length: "+newfileLength+"\r\n"+											 
											"Connection: close"+"\r\n\r\n";

									dataOut.write(str.getBytes());
								}

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
								}else if(headerMap.containsKey("Range")){
									System.out.println("file for Range bytes"+fileRequested + "bytes in length :"+totalLengthForPartialContent);
									dataOut.write(getPartialContent(fileRequested,totalLengthForPartialContent));
								}

								else if(fileRequested.endsWith("koi8-r")){
									System.out.println("fileRequested 1112233 :"+fileRequested);
									File f1 = new File(fileRequested);
									InputStream inp = new FileInputStream(f1);
									byte[] buffer=new byte[1024];
									int readData;
									while((readData = inp.read(buffer))!=-1){
										dataOut.write(buffer,0,readData);
									}
									dataOut.flush();	
								}

								else if(!fileRequested.contains(".gif") && !fileRequested.contains(".jpeg") && newfileLength != 0){
									System.out.println("fileRequested 11122 :"+fileRequested);
									String filePath1 = new File(fileRequested).getAbsolutePath().replace("src/../","");
									filePath1 = filePath1.replace("src/../","");									
									FileReader fr = new FileReader(filePath1);
									BufferedReader br = new BufferedReader(fr);
									String fileLine;
									while ((fileLine = br.readLine()) != null) {
										dataOut.write((fileLine+"\r\n").getBytes());
									}	
								}


							}else{
								System.out.println("here for 404");
								out.print("HTTP/1.1 404 Not Found"+"\r\n");  
								out.print("Date: "+formatted+"\r\n"); 
								out.print("Server: "+host+"\r\n");
								out.print("Connection: close"+"\r\n");
								out.print("Content-Type: "+content+"\r\n");								
								out.print("\r\n");
							}
						}
						else if(needsRedirection){
							// nothing to handle
						}else if(isTransferEncoding){
							System.out.println("here2 for 404");
							out.print("HTTP/1.1 404 Not Found"+"\r\n");  
							out.print("Date: "+formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("Content-Type: "+content+"\r\n");	
							out.print("Transfer-Encoding: chunked"+"\r\n");
							out.print("Connection: close"+"\r\n");
							out.print("\r\n");

							out.print(getChunkedBytes("../public/a3-test/404.html"));
							out.print("\r\n\r\n");
						}
						else{
							System.out.println("here3 for 404");
							out.print("HTTP/1.1 404 Not Found"+"\r\n");  
							out.print("Date: "+formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("Content-Type: "+content+"\r\n");								
							out.print("\r\n");
						}
					}
				}

				if (method.equals("HEAD")) { 
					String regexPath = "";
					System.out.println("INSIDE HEAD :"+fileRequested);
					boolean isFileAvailable = checkAvailability(fileRequested);
					System.out.println("IN HEAD :"+isFileAvailable);
					System.out.println("noAcceptHeader :"+noAcceptHeader);
					long newfileLength = 0l;
					int countRepeats = 0;
					String[] acceptHeaders;
					String[] acceptHeaderCharSet;
					String[] acceptHeaderLang;
					String requiredExtension = "", extension = "", valueStr = "",realExtension = "";
					float higherValue = 0.0f;
					float currentValue = 0.0f;
					if(headerMap.containsKey("Accept") && !fileRequested.endsWith(".Z")){
						System.out.println("Accept in HEAD"+fileRequested);
						acceptHeaders = headerMap.get("Accept").split(",");
						for(String header: acceptHeaders){
							valueStr = header.split(";")[1];
							currentValue = Float.valueOf(valueStr.substring(valueStr.indexOf("=")+1));
							if(currentValue > higherValue){
								higherValue = currentValue;
								requiredExtension = header.split(";")[0];
								extension = requiredExtension.substring(requiredExtension.indexOf("/")+1);
								if(extension.equals("*")){
									extension = requiredExtension.substring(0,requiredExtension.indexOf("/"));
									if(extension.equals("text") && checkAvailability(fileRequested+".txt")){
										System.out.println("Ext is TXT");
										realExtension = "txt";
									}else if(extension.equals("image") && (checkAvailability(fileRequested+".png"))){
										realExtension = "png";
									}else if(extension.equals("image") && (checkAvailability(fileRequested+".jpeg"))){
										realExtension = "jpeg";
									}else if(extension.equals("image") && (checkAvailability(fileRequested+".jpg"))){
										realExtension = "jpg";
									}
								}else if(checkAvailability(fileRequested+"."+extension)){
									realExtension = extension;
								}
							}else if(currentValue == higherValue){
								countRepeats++;
							}
							if(countRepeats > 1){
								zeroQValue = true;
							}
						}
						System.out.println("Accept Header fileRequested :"+fileRequested+"."+realExtension);
						fileRequested = fileRequested +"."+realExtension;

						isFileAvailable = checkAvailability(fileRequested);
						System.out.println("IS AVAILABLE :"+isFileAvailable);
					}

					else if(headerMap.containsKey("Accept-Encoding") && !fileRequested.endsWith(".Z")){

						acceptHeaders = headerMap.get("Accept-Encoding").split(",");	

						for(String header: acceptHeaders){
							valueStr = header.split(";")[1];
							currentValue = Float.valueOf(valueStr.substring(valueStr.indexOf("=")+1));
							if(currentValue > higherValue){
								higherValue = currentValue;
								requiredExtension = header.split(";")[0];
								extension = requiredExtension.substring(requiredExtension.indexOf("/")+1);
								if(extension.equals("*")){
									extension = requiredExtension.substring(0,requiredExtension.indexOf("/"));
									if(extension.equals("text") && checkAvailability(fileRequested+".txt")){
										realExtension = "txt";
									}else if(extension.equals("image") && (checkAvailability(fileRequested+".png"))){
										realExtension = "png";
									}else if(extension.equals("image") && (checkAvailability(fileRequested+".jpeg"))){
										realExtension = "jpeg";
									}else if(extension.equals("image") && (checkAvailability(fileRequested+".jpg"))){
										realExtension = "jpg";
									}
								}else if(checkAvailability(fileRequested+"."+extension)){
									realExtension = extension;
								}
							}else if(currentValue == higherValue){
								countRepeats++;
							}
							if(countRepeats > 1){
								zeroQValue = true;
							}
						}
						System.out.println("Accept Header fileRequested :"+fileRequested+"."+realExtension);
						fileRequested = fileRequested +"."+realExtension;

						isFileAvailable = checkAvailability(fileRequested);
					}else if(headerMap.containsKey("Accept-Charset") && headerMap.containsKey("Accept-Language")){

						HashMap<String,Float> charSetMap = new HashMap<String,Float>();
						acceptHeaderCharSet = headerMap.get("Accept-Charset").split(",");	

						for(String header: acceptHeaderCharSet){
							setString = header.split(";")[0].trim(); 
							valueStr = header.split(";")[1].trim();
							currentValue = Float.valueOf(valueStr.substring(valueStr.indexOf("=")+1));
							charSetMap.put(setString, currentValue);
						}
						System.out.println("MAP CONTENTS : "+charSetMap);
						higherValue = charSetMap.get("iso-2022-jp");
						Iterator itr = charSetMap.entrySet().iterator();
						while (itr.hasNext()) {
							Map.Entry pair = (Map.Entry)itr.next();
							if(((String)pair.getKey()).equals(reqSet)){
								System.out.println("VALUE TEST : "+(String)pair.getKey());
								continue;
							}else{
								float testValue = (Float)pair.getValue();
								System.out.println("VALUE : "+testValue);
								if(testValue > higherValue){
									System.out.println("SETS 406");
									send406 = true;
								}
							}

						}
						//						System.out.println("Accept Header fileRequested :"+fileRequested+"."+realExtension);
						//						fileRequested = fileRequested +"."+realExtension;
						//
						//						isFileAvailable = checkAvailability(fileRequested);
					}else{
						System.out.println("Here in else :"+fileRequested);
						isFileAvailable = checkAvailability(fileRequested);
					}

					if(fileRequested.matches("^(.*)/1.[234]/(.*)")){
						fileRequested = fileRequested.replace("../public/","");
						regexPath = fileRequested.substring(fileRequested.indexOf("/"),fileRequested.lastIndexOf("/"));
						System.out.println("INSIDE HEAD regexPath :"+regexPath);
						while(!regexPath.matches("/1.[234]")){
							regexPath = regexPath.substring(regexPath.lastIndexOf("/"));
						}
						System.out.println("regexPath MATCHED:"+regexPath);
						fileRequested = "/"+fileRequested.replace(regexPath, "/1.1");
						System.out.println("After REGEX :"+fileRequested);
						out.print("HTTP/1.1 302 Found"+"\r\n");
						out.print("Date: "+formatted+"\r\n"); 
						out.print("Server: "+host+"\r\n");
						out.print("Content-Type: "+content+"\r\n");
						out.print("Location: "+fileRequested+"\r\n");
						out.print("Connection: close"+"\r\n\r\n");
					}else if(send406){
						System.out.println("fileRequested in send406 :"+fileRequested);

						out.print("HTTP/1.1 406 Not Acceptable"+"\r\n");
						out.print("Date: "+formatted+"\r\n"); 
						out.print("Server: "+host+"\r\n");
						out.print("Content-Type: "+content+"\r\n");
						out.print("Transfer-Encoding: chunked"+"\r\n");
						out.print("Alternatives: "+getAlternatives(fileRequested)+"\r\n");
						out.print("TCN: list"+"\r\n");
						out.print("Vary: negotiate,accept-encoding"+"\r\n");
						out.print("Connection: close"+"\r\n\r\n");
					}

					else if(zeroQValue){
						System.out.println("fileRequested in zeroQValue :"+fileRequested);

						out.print("HTTP/1.1 406 Not Acceptable"+"\r\n");
						out.print("Date: "+formatted+"\r\n"); 
						out.print("Server: "+host+"\r\n");
						out.print("Content-Type: "+content+"\r\n");
						out.print("Transfer-Encoding: chunked"+"\r\n");
						out.print("Location: "+fileRequested+"\r\n");
						out.print("Connection: close"+"\r\n\r\n");
					}else if(acceptHeader){

						System.out.println("Here in accept header : "+fileRequested);
						if(fileRequested.endsWith(".txt")){
							File fileForLength = new File(fileRequested);
							newfileLength = fileForLength.length();
							out.print("HTTP/1.1 200 OK"+"\r\n");  									 
							out.print("Date: " +formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("Content-Length: "+newfileLength+"\r\n");
							out.print("Content-Type: "+getContentType(fileRequested,"HEAD")+"\r\n");
							out.print("Transfer-Encoding: chunked"+"\r\n");
							out.print("Connection: close"+"\r\n"); 
							out.print("\r\n");	


						}
						else if(fileRequested.endsWith("fairlane")){
							File fileForLength = new File(fileRequested+".txt");
							newfileLength = fileForLength.length();
							out.print("HTTP/1.1 300 Multiple Choice"+"\r\n");  									 
							out.print("Date: " +formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("Content-Length: "+newfileLength+"\r\n");
							out.print("Content-Type: "+getContentType(fileRequested,"HEAD")+"\r\n");
							out.print("Transfer-Encoding: chunked"+"\r\n");
							out.print(getVaryString()+"\r\n");
							out.print("TCN: list");
							out.print("Connection: close"+"\r\n"); 
							out.print("\r\n");	


						}else if(fileRequested.endsWith("fairlane")){
							File fileForLength = new File(fileRequested+".txt");
							newfileLength = fileForLength.length();
							out.print("HTTP/1.1 300 Multiple Choice"+"\r\n");  									 
							out.print("Date: " +formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("Content-Length: "+newfileLength+"\r\n");
							out.print("Content-Type: "+getContentType(fileRequested,"HEAD")+"\r\n");
							out.print("Transfer-Encoding: chunked"+"\r\n");
							out.print(getVaryString()+"\r\n");
							out.print("TCN: list"+"\r\n");
							out.print("Connection: close"+"\r\n"); 
							out.print("\r\n");	


						}else{
							System.out.println("Here ... :"+fileRequested);
							File fileForLength = new File(fileRequested+".en");
							newfileLength = fileForLength.length();
							out.print("HTTP/1.1 300 Multiple Choice"+"\r\n");  									 
							out.print("Date: " +formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("Content-Length: "+newfileLength+"\r\n");
							out.print("Content-Type: "+getContentType(fileRequested,"HEAD")+"\r\n");
							out.print("Transfer-Encoding: chunked"+"\r\n");
							out.print(getVaryString()+"\r\n");
							out.print("TCN: list"+"\r\n");
							out.print("Connection: close"+"\r\n"); 
							out.print("\r\n");	

						}
					}
					else if(if_mod_flag && isFileAvailable){
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

							if(modDate.compareTo(currDate) <= 0 && fileRequested.contains("fairlane.gif")) {
								System.out.println("modDate inside 304:");
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
							}else if(modDate.compareTo(currDate) <= 0 ){
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
							System.out.println("File 11 :"+fileRequested);
							String filePath = new File(fileRequested).getAbsolutePath().replace("src/../","");
							File fileForLength = new File(filePath);
							newfileLength = fileForLength.length();
							out.print("HTTP/1.1 200 OK"+"\r\n");
							out.print("Date: "+formatted+"\r\n"); 
							out.print("Server: "+host+"\r\n");
							out.print("Content-Type: "+content+"\r\n");
							out.print("Last-Modified: "+getLastModified(fileRequested)+"\r\n");		
							out.print("ETag: "+"\""+generateETag(fileRequested)+"\""+"\r\n");
							out.print("Content-Length: "+newfileLength+"\r\n");										 
							out.print("Connection: close"+"\r\n\r\n");  

						}

					}
					else if(isFileAvailable){
						System.out.println("HERE NOW :"+keepAlive);
						System.out.println("fileRequested HEAD: "+fileRequested);
						if(fileRequested.charAt(fileRequested.length()-1) == '.'){
							fileRequested = fileRequested.substring(0, fileRequested.length()-1);
						}
						String filePath = new File(fileRequested).getAbsolutePath().replace("src/../","");
						System.out.println("filePath : "+filePath);
						File f2 = new File(fileRequested);
						if(f2.exists() && !f2.isDirectory()) { 
							File fileForLength = new File(filePath);
							newfileLength = fileForLength.length();
						}else if(f2.isDirectory()){
							newfileLength = 0;
						}

						//						if(keepAlive){
						//							keepAliveStr = "Connection: keep-alive"; 
						//						}else{
						//							keepAliveStr = "Connection: close";  
						//						}

						System.out.println("is Alive Now : "+client.getKeepAlive());

						out.print("HTTP/1.1 200 OK"+"\r\n");
						out.print("Server: "+host+"\r\n");
						out.print("Date: "+formatted+"\r\n"); 						
						out.print("Content-Type: "+getContentType(fileRequested, "HEAD")+"\r\n");
						out.print("ETag: "+"\""+generateETag(fileRequested)+"\""+"\r\n");						
						out.print("Last-Modified: "+getLastModified(filePath)+"\r\n");	
						out.print("Content-Location: "+fileRequested+"\r\n");
						out.print("Content-Length: "+newfileLength+"\r\n");	
						if(headerMap.containsKey("Accept")){
							out.print(getVaryString()+"\r\n");
							out.print("TCN: choice"+"\r\n");
						}
						if(fileRequested.contains(".Z") || fileRequested.contains(".gz")){
							out.print("Content-Encoding: "+getContentEncoding(fileRequested)+"\r\n");	
						}
						out.print("Connection: close"+"\r\n\r\n"); 
						//Thread.sleep(2000);



					}else if(noAcceptHeader){
						System.out.println("HEAD noAcceptHeader");
						File fileForLength = new File(fileRequested+".html");
						newfileLength = fileForLength.length();
						out.print("HTTP/1.1 300 Multiple Choice"+"\r\n");  									 
						out.print("Date: " +formatted+"\r\n"); 
						out.print("Server: "+host+"\r\n");
						out.print("Content-Length: "+newfileLength+"\r\n");
						out.print("Content-Type: "+getContentType(fileRequested+".html", "HEAD")+"\r\n");
						out.print("Transfer-Encoding: chunked"+"\r\n");
						out.print("Connection: close"+"\r\n"); 
						out.print("\r\n");	
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

					//					if(method.equals("TRACE")){
					//						fileRequested = fileRequested.replace("index.html","");
					//					}

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

				//				if(!client.getKeepAlive() && !headerMap.containsKey("Range")){
				//					out.print("HTTP/1.1 408 Request Timeout\r\n");
				//					out.print("Date: " +formatted+"\r\n"); 
				//					out.print("Server: "+host+"\r\n");					 
				//					out.print("Connection: close"+"\r\n\r\n");
				//				}


				out.close();
				dataOut.close();
				in.close();
				client.close();
			}
		}
		catch(SocketTimeoutException se){

			System.out.println("Here closed");
			out.print("HTTP/1.1 408 Request Timeout"+"\r\n");
			//out.print("Date: " +formatted+"\r\n"); 
			out.print("Server: "+host+"\r\n");					 
			out.print("Connection: close"+"\r\n\r\n");
			out.flush();
			//client.close();

		}
		catch (Exception e) {
			e.printStackTrace();
		}

		long endTime = System.currentTimeMillis();

	}


	public static String generateETag(String file) throws NoSuchAlgorithmException, IOException{
		System.out.println("In ETag : "+file);
		//		file = file.substring(file.indexOf("//")+2);
		//		file = file.substring(file.indexOf("/"));

		File f = new File(file);
		if(f.isDirectory()){
			return "";
		}
		if(!file.contains("../public")){
			file = "../public" + file;
		}
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(Files.readAllBytes(Paths.get(file)));
		byte[] digest = md.digest();
		System.out.println("Etag : "+DatatypeConverter.printHexBinary(digest).toUpperCase());
		return DatatypeConverter.printHexBinary(digest).toUpperCase();
	}

	public static byte[] getPartialContent(String file,int numBytes) throws IOException{
		File fileName = new File(file);
		InputStream in = new FileInputStream(fileName);
		return readByteBlock(in,0,numBytes);
	}

	public static byte[] readByteBlock(InputStream in, int offset, int noBytes) throws IOException {
		byte[] result = new byte[noBytes];
		in.read(result, offset, noBytes);
		return result;
	}

	public static String getContentEncoding(String file){
		if(file.endsWith("Z")){
			return "compress";
		}else if(file.endsWith(".gz")){

		}
		return "compress";
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

	public static boolean checkAvailability(String fileName){
		System.out.println("Check Avail :"+fileName);
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
		}else if(fileName.contains("../public") && (fileName.endsWith(".Z") || fileName.endsWith(".gz"))){
			directory = fileName.substring(0,fileName.lastIndexOf('/'));
			if(fileName.charAt(fileName.length()-1) == '.'){
				fileName = fileName.substring(0, fileName.length()-1);
			}
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
		else if(directory.contains("/public/a3-test")){
			System.out.println("Here 11"+fileExists(currentDir,filename));
			return fileExists(currentDir,filename);
			//return displayDirectoryContents(currentDir,filename);
		}

		return displayDirectoryContents(currentDir,filename);

	}

	public static boolean fileExists(File fileText,String fileName){
		System.out.println("fileName IS :"+fileName);
		System.out.println("is Dir :"+fileText.isDirectory());
		System.out.println("File in exists :"+(fileText.getAbsolutePath().replace("src/../", "")));
		String fname = (fileText.getAbsolutePath()).replace("src/../", "");
		if(fname.charAt(fname.length()-1) == '.'){
			fname = fname.substring(0, fname.length()-1);
		}
		System.out.println("fname :"+fname+"/"+fileName);
		System.out.println("fileName :"+fileName);
		File f = new File(fname+"/"+fileName);
		System.out.println("File is :"+f.exists());
		try{
			if(fileText.exists() && !fileText.isDirectory()) { 
				return true;
			}else if(f.exists()){
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
		System.out.println("getContentType method :"+fileRequested);
		if(method.equals("TRACE")){
			return "message/http";
		}else if(fileRequested.contains("koi8-r")){
			System.out.println("Im here");
			return "text/html;" + " charset=koi8-r";
		}else if(fileRequested.endsWith("protected")){
			return "application/octet-stream";
		}
		else if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html") || fileRequested.contains(".html.") || fileRequested.contains(".html"))
			return "text/html";
		else if(fileRequested.contains("directory3isempty")){
			return "application/octet-stream";
		}
		else if (fileRequested.endsWith(".xml")  ||  fileRequested.endsWith(".XML")){
			return "text/xml";
		}else if (fileRequested.endsWith(".jpeg")  ||  fileRequested.endsWith(".jpg")){
			return "image/jpeg";
		}else if (fileRequested.endsWith(".png")){
			return "image/png";
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

		System.out.println("File :"+filePath);
		if(!filePath.contains("public")){
			filePath = "../public" + filePath;
		}
		File file = new File(filePath);

		System.out.println("is Directory : "+file.isDirectory());
		if(file.exists() && file.isDirectory()){
			Date lastModified = new Date(file.lastModified());
			Calendar calendar = Calendar.getInstance();
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			System.out.println("Directorry LM :"+file.lastModified());
			return dateFormat.format(lastModified);
		}else{
			Date lastModified = new Date(file.lastModified());
			Calendar calendar = Calendar.getInstance();
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			System.out.println("LAST MODIFIED :"+dateFormat.format(lastModified));
			return dateFormat.format(lastModified);
		}
	}

	public static String getContentLanguage(String fileText){
		if (fileRequested.endsWith(".en")){
			return "en";
		}else if(fileRequested.endsWith(".es")){
			return "es";
		}else if( fileRequested.endsWith(".de")){
			return "de";
		}else if( fileRequested.contains(".ja.")){
			return "ja";
		}else if( fileRequested.contains(".ko.")){
			return "ko";
		}else if( fileRequested.contains(".ru.")){
			return "ru";
		}else if( fileRequested.contains(".ru.")){
			return "ru";
		}else{
			return "English";
		}

	}

	public static String getAlternatives(String fileName){
		String alternativeStr = "";
		String contentType = getContentType(fileRequested, "HEAD");

		String fileInLoop = "", allFiles = "";
		String dirName = fileName.substring(0, fileName.lastIndexOf("/")); 
		String file = fileName.substring(fileName.lastIndexOf("/")+1);
		String language = file.substring(file.lastIndexOf(".")+1);
		File dir = new File(dirName);
		System.out.println("Dir :"+dir.getName()+" :LAN :"+language);
		File[] files = dir.listFiles();
		for(File filename : files){
			System.out.println(filename.length());
			fileInLoop = filename.getName();
			if(fileInLoop.contains("."+language+".")){
				System.out.println(fileInLoop);

				alternativeStr += "{" + "\"" + fileInLoop+ "\"" + " ";
				alternativeStr += "1" + " {type " + getContentType(fileInLoop, "HEAD") + "}" ; 
				alternativeStr += " {charset " + reqSet +"}"  ; 
				alternativeStr += " {language " + language +"}"  ; 
				alternativeStr += " {length " + filename.length() +"}"  ;
			}
		}

		return alternativeStr;
	}

	public static boolean confirmAuthorization(String file,String password) throws IOException{
		System.out.println("File in confirmAuthorization 1 :"+file);
		FileInputStream fstream = new FileInputStream("../public/Basic.txt");
		BufferedReader brRead = new BufferedReader(new InputStreamReader(fstream));
		boolean isConfirmed = false;
		String strLine = "", pass ="" , str ="";
		
		while ((strLine = brRead.readLine()) != null)   {
			System.out.println("File in confirmAuthorization 3 :"+file);
			file = file.substring(file.lastIndexOf("/")+1);
			str = strLine.split(" ")[0];
			System.out.println("File in confirmAuthorization 2 :"+strLine+" "+file);
			
			pass = strLine.split(" ")[1];
			System.out.println("File in confirmAuthorization 4 :"+file);
			System.out.println("File in confirmAuthorization 4 :"+strLine+" "+pass);
			if(file.equals(str) && pass.equals(password.split(" ")[1])){
				isConfirmed = true;
				break;
			}
		}
		brRead.close();
		return isConfirmed;
	}
	
	public static String getVaryString(){
		if(headerMap.containsKey("Accept")){
			return "Vary: negotiate,accept";
		}else if(headerMap.containsKey("Accept-Encoding")){
			return "Vary: negotiate,accept-encoding";
		}else if(headerMap.containsKey("Accept-Language")){
			return "Vary: negotiate,accept-charset,accept-language";
		}else{
			return "Vary: negotiate,accept";
		}
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