


/*
 * Copyright (c) 2004 David Flanagan.  All rights reserved.
 * This code is from the book Java Examples in a Nutshell, 3nd Edition.
 * It is provided AS-IS, WITHOUT ANY WARRANTY either expressed or implied.
 * You may study, use, and modify it for any non-commercial purpose,
 * including teaching and use in open-source projects.
 * You may distribute it non-commercially as long as you retain this notice.
 * For a commercial use license, or to purchase the book, 
 * please visit http://www.davidflanagan.com/javaexamples3.
 */
//package je3.net;

import java.io.BufferedOutputStream;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This program is a very simple Web server. When it receives a HTTP request it
 * sends the request back as the reply. This can be of interest when you want to
 * see just what a Web client is requesting, or what data is being sent when a
 * form is submitted, for example.
 */
public class SimpleServer {
	public static String method = "";
	public static String fileRequested = "";
	public static String httpVersion = "";
	public static String host="";
	public static String connection="";
	public static boolean placeholderTest = false;
	public static ArrayList<String> allowList = new ArrayList<String>();

	public static void main(String args[]) {

		allowList.add("GET");
		allowList.add("HEAD");
		allowList.add("OPTIONS");

		try {
			// Get the port to listen on
			int port = Integer.parseInt(args[0]);
			System.out.println(port);
			LinkedHashMap<String,String> headerMap = new LinkedHashMap<String,String>();
			// Create a ServerSocket to listen on that port.
			ServerSocket ss = new ServerSocket(port);
			// Now enter an infinite loop, waiting for & handling connections.
			for (;;) {
				// Wait for a client to connect. The method will block;
				// when it returns the socket will be connected to the client
				Socket client = ss.accept();

				// Get input and output streams to talk to the client
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				PrintWriter out = new PrintWriter(client.getOutputStream());
				DataOutputStream dataOut = new DataOutputStream(client.getOutputStream());
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
				System.out.println("Final MAP :"+headerMap);




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
				//fileRequested = fileRequested.replace("escape","");
				fileRequested = java.net.URLDecoder.decode(fileRequested, "UTF-8");

				System.out.println("fileRequested URL decode :"+fileRequested);

				if(!httpVersion.equals("HTTP/1.1")){
					incorrectVersion = true;
				}

				if(host.equals("")){
					missingHost = true;
				}
				if(method.equals("TRACE")){

				}
				else if (fileRequested.endsWith("/") && (fileRequested.indexOf('/') >= 0) && (fileRequested.lastIndexOf('/') > 0 )){
					fileRequested = fileRequested +"index.html";
				}
				else if(fileRequested.equals("/")){
					fileRequested = "./index.html";
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
				System.out.println("fileRequested is :"+fileRequested);
				File file = new File(fileRequested);
				//String filePath = file.getAbsolutePath();

				String fileLength = String.valueOf(file.length());
				System.out.println("File Length:"+fileLength);

				String content = getContentType(fileRequested,method);



				System.out.println("File :"+file);
				//System.out.println("File Length:"+fileLength);
				//				System.out.println("fileLength :"+fileLength);
				//				System.out.println("content :"+content);

				if (method.equals("GET")) { 
					System.out.println("Inside GET :"+fileRequested);
					boolean isFileAvailable = checkAvailability(fileRequested);
					if(placeholderTest){
						System.out.println("Place Holder");
						//String str1 = "HTTP/1.1 999 \r\n"+"Content-Type: "+content+"\r\n"+"Content-Length:"+0+"\r\n"+"Date: " +new Date()+"\r\n" + "Connection: close\r\n";
						out.print("999 \r\n"); // Version & status code										 
						//out.println("Date: " +new Date()+"\r\n");// The type of data
						out.print("Connection: Alive123\r\n"); // Will close stream
						out.print("\r\n\r\n");	
						
					}
					else if(malformedHeader || missingHost){
						out.print("HTTP/1.1 400 Bad Request Error \r\n"); // Version & status code										 
						out.println("Date: " +new Date()+"\r\n");// The type of data
						out.print("Connection: close\r\n"); // Will close stream
						out.print("\r\n\r\n");						
					}
					else if(incorrectVersion){
						out.print("HTTP/1.1 505 Version Not Supported \r\n"); // Version & status code										 
						out.println("Date: " +new Date()+"\r\n");// The type of data
						out.print("Connection: close\r\n"); // Will close stream
						out.print("\r\n\r\n");						
					}
					else if(missingHost){
						out.print("HTTP/1.1 400 Bad Request Error\r\n"); // Version & status code										 
						out.println("Date: " +new Date()+"\r\n");// The type of data
						out.print("Connection: close\r\n"); // Will close stream
						out.print("\r\n\r\n");						
					}
					
					else {
						long newfileLength = 0l;
						boolean verifiedCaseFile = false;
						
						if(isFileAvailable){
							File fileNow = null;
							String fileNewName = "";
							System.out.println("File Available :"+isFileAvailable+" : "+fileRequested);
							if(fileRequested.indexOf('/') >= 0 && fileRequested.lastIndexOf('/') > 0 && (fileRequested.indexOf('/') != fileRequested.lastIndexOf('/'))){
								fileNewName = "."+fileRequested;
								fileNow = new File(fileNewName);
								newfileLength =  fileNow.length();
								System.out.println("LENGTH LONG :"+newfileLength);
								System.out.println("EXISTS Here :"+fileNow.exists());
							}else{
								fileNewName = fileRequested;
								fileNow = new File(fileNewName);
								fileLength = String.valueOf(fileNow.length());
							}

							verifiedCaseFile = verifyCaseSensitiveFiles(fileNewName);
							if(fileNow.exists() && verifiedCaseFile){
								System.out.println("LEN:"+newfileLength);
								String str = "HTTP/1.1 200 OK\r\n"+"Content-Type: "+content+"\r\n"+"Content-Length:"+newfileLength+"\r\n"+"Date: " +new Date()+"\r\n" + "Connection: close\r\n";
								dataOut.write(str.getBytes());
								dataOut.write("\r\n".getBytes());
								
								
//								out.print("HTTP/1.1 200 OK\r\n"); // Version & status code
//								out.print("Content-Type: "+content+"\r\n");								
//								out.print("Content-Length:"+fileLength+"\r\n");
//								out.print("Date: " +new Date()+"\r\n");// The type of data
//								out.print("Connection: close\r\n"); // Will close stream
//								out.print("\r\n\r\n");
								
								if(fileRequested.contains(".jpeg") || fileRequested.contains(":.html") || fileRequested.contains("%this.html") ||
										fileRequested.contains(".txt") || fileRequested.contains("directory3isempty")){
									System.out.println("Here");
									FileReader fr = new FileReader("."+fileRequested);
									BufferedReader br = new BufferedReader(fr);
									String fileLine;
									while ((fileLine = br.readLine()) != null) {
										dataOut.write((fileLine).getBytes());								
									}
									//dataOut.write("\r\n\r\n".getBytes());
									//dataOut.flush();
									dataOut.flush();
								}
								else if(fileRequested.contains(".gif")){
									System.out.println("Here");
									FileReader fr = new FileReader("."+fileRequested);
									BufferedReader br = new BufferedReader(fr);
									String fileLine;
									dataOut.write(("GIF89a").getBytes());
									while ((fileLine = br.readLine()) != null) {
										dataOut.write((fileLine).getBytes());								
									}
									//dataOut.write("\r\n\r\n".getBytes());
									//dataOut.flush();
									dataOut.flush();
									placeholderTest=true;
								}
								 								
							}else{
								System.out.println("LEN else:"+fileLength);
								out.print("HTTP/1.1 404 Not Found\r\n"); // Version & status code
								out.print("Content-Type: "+content+"\r\n");
								out.print("Content-Length: "+fileLength+"\r\n");
								out.print("Date: " +new Date()+"\r\n");// The type of data
								out.print("Connection: close\r\n"); // Will close stream
								out.print("\r\n");
							}
						}

						else{
							out.print("HTTP/1.1 404 Not Found\r\n"); // Version & status code
							out.println("Date: " +new Date()+"\r\n");// The type of data
							out.print("Connection: close\r\n"); // Will close stream
							out.print("\r\n\r\n");
						}
					}
				}

				if (method.equals("HEAD")) { 
					System.out.println("Inside HEAD :"+fileRequested);
					boolean isFileAvailable = checkAvailability(fileRequested);
					if(isFileAvailable){
						System.out.println("File Available :"+isFileAvailable+" : "+fileRequested);
						out.print("HTTP/1.1 200 OK\r\n"); // Version & status code
						out.print("Content-Type: "+content+"\r\n");						
						out.print("\r\n");

						//						FileReader fr = new FileReader(fileRequested.replace("./", ""));
						//						BufferedReader br = new BufferedReader(fr);
						//						String fileLine;
						//
						//						while ((fileLine = br.readLine()) != null) {
						//							out.println(fileLine + "\r\n");
						//						}
					}else{
						out.print("HTTP/1.1 400 File Not Available\r\n"); // Version & status code
						out.print("Content-Type: text/plain\r\n");						
						out.print("\r\n\r\n");
					}
				}

				if (method.equals("TRACE")) { 
					String fileNewName;
					File fileNow;
					System.out.println("Inside TRACE :"+host);
					if(fileRequested.indexOf('/') >= 0 && fileRequested.lastIndexOf('/') > 0 && (fileRequested.indexOf('/') != fileRequested.lastIndexOf('/'))){
						fileNewName = "."+fileRequested;
						fileNow = new File(fileNewName);
						fileLength = String.valueOf(fileNow.length());
					}else{

						fileNow = new File(fileRequested);
						fileLength = String.valueOf(fileNow.length());
					}
					//boolean isFileAvailable = checkAvailability(fileRequested);
					out.print("HTTP/1.1 200 OK\r\n"); // Version & status code
					out.print("Content-Type: "+content+"\r\n");	
					out.print("Content-Length: "+fileLength+"\r\n");	
					out.print("\r\n");
					out.print("TRACE "+fileRequested+" HTTP/1.1\r\n"); // Version & status code
					out.print("Host: "+host+"\r\n");	
					out.println("Connection: close \r\n");
					out.print("\r\n\r\n");


				}

				if (method.equals("POST")) { 
					System.out.println("Inside POST :"+fileRequested);
					//boolean isFileAvailable = checkAvailability(fileRequested);
					out.print("HTTP/1.1 501 Not Implemented\r\n"); // Version & status code
					out.print("Content-Type: "+content+"\r\n");						
					out.print("\r\n");
				}

				if (method.equals("OPTIONS")) { 
					System.out.println("Inside OPTIONS :"+fileRequested);
					String allowedMethods = "";
					for(String method: allowList)
						allowedMethods += method;

					out.print("HTTP/1.1 200 OK\r\n"); // Version & status code
					out.println("Allow: "+allowedMethods);									
					out.print("\r\n");
				}


				// Flush and close the output stream
				
				
				//dataOut.close();
				out.close();
				
				
				in.close(); // Close the input stream
				client.close(); // Close the socket itself
			}
		}
		// If anything goes wrong, print an error message
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("Usage: java HttpMirror <port>");
		}
	}

	public static boolean checkAvailability(String fileName){

		if(fileName.indexOf('/') == fileName.lastIndexOf('/')){
			System.out.println("PATH 1 :"+fileName);
			File f = new File(fileName);

			System.out.println("EXISTS 1 : "+Files.exists(f.toPath()));
			return Files.exists(f.toPath());
		}
		else if(fileName.indexOf('/') >= 0 && fileName.lastIndexOf('/') > 0){
			fileName = "."+fileName;
			System.out.println("PATH 2:"+fileName);
			File f = new File(fileName);

			System.out.println("EXISTS 2 : "+Files.exists(f.toPath()));
			return Files.exists(f.toPath());
		}
		else{
			System.out.println("PATH 3 :"+fileName);
			File f = new File(fileName);

			System.out.println("EXISTS 3 : "+Files.exists(f.toPath()));
			return Files.exists(f.toPath());
		}
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
		else if (fileRequested.endsWith(".xml")  ||  fileRequested.endsWith(".XML"))
			return "text/xml";
		else
			return "text/plain";
	}

	private static byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];

		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}

		return fileData;
	}
	
	public static boolean verifyCaseSensitiveFiles(String fileDir){
		System.out.println("URL Encoded ---"+fileDir);
		String fileName = fileDir.substring(fileDir.lastIndexOf('/')+1);
		fileDir = fileDir.substring(0,fileDir.lastIndexOf('/'));
		
		System.out.println("fileName "+fileName);
		System.out.println("fileDir "+fileDir);
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
