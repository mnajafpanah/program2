/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;
import java.nio.file.Files;
import java.text.SimpleDateFormat;

public class WebWorker implements Runnable
{

    private Socket socket;
    //String code;
    String format;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   String HTML; //location
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      HTML = readHTTPRequest(is);
      //readHTTPRequest(is);
      
       //check the file's format
       if(HTML.endsWith(".jpg")) format = "image/jpg"; // jpg format
       
       else if (HTML.endsWith(".gif")) format = "image/gif"; // gif format
       
       else if(HTML.endsWith(".png")) format = "image/png"; // png format
       
       else if(HTML.endsWith(".ico")) format = "image/x-icon"; // ico format(extra credit)
       
       else {format = "text/html";} // text format
       
       writeHTTPHeader(os, format, HTML);
      
       writeContent(os, HTML);
      
       os.flush();
      
       socket.close();
       
   } catch (Exception e) {
       
      System.err.println("Output error: " + e);
   }
    
   System.err.println("Done handling connection.");
   
    return;
    
}//end run
    
    
    

/**
* Read the HTTP request header.
**/
private String readHTTPRequest(InputStream is)
    {
        String line;
        String HTMLString = " ";
        BufferedReader b = new BufferedReader(new InputStreamReader(is));
        while (true) {
            try {
                while(!b.ready()) Thread.sleep(1);
                line = b.readLine();
                System.err.println("Request line: ("+line+")");
                
                String local = line.substring(0,3);
                
                if(local.equals("GET")){ //if the line is GET
                    
                    HTMLString = line.substring(4); //reminder of string
                    
                    HTMLString = HTMLString.substring(0, HTMLString.indexOf(" "));
                    
                    System.err.println("Requested file is: " + HTMLString);
                }
                
                if (line.length() == 0) break;
                
            } catch (Exception e) {
                System.err.println("Request error: "+e);
                break;
            }
        }
        
        return HTMLString;
    
    }// end readHTTPRequest
    
    

    
/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType, String HTML) throws Exception
{
    //System.out.println("in writeHTTPheader");

    Date d = new Date();
   
    DateFormat df = DateFormat.getDateTimeInstance();
   
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    
    File f = new File(HTML);
    
    //identify whether file exists or not
    if(f.exists() && !f.isDirectory()){
   
    os.write(("HTTP/1.1 200 OK\n").getBytes());
   
    os.write("Date: ".getBytes());
   
    os.write((df.format(d)).getBytes());
   
    os.write("\n".getBytes());
   
    os.write("Server: Jon's very own server\n".getBytes());
   
    //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   
    //os.write("Content-Length: 438\n".getBytes());
   
    os.write("Connection: close\n".getBytes());
   
    os.write("Content-Type: ".getBytes());
   
    os.write(contentType.getBytes());
   
    os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
        
    } else{//otherwise file does not exist
        
        os.write(("HTTP/1.1 404 Not Found\n").getBytes());
        
        os.write("Date: ".getBytes());
        
        os.write((df.format(d)).getBytes());
        
        os.write("\n".getBytes());
        
        os.write("Server: Jon's very own server\n".getBytes());
        
        //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
        
        //os.write("Content-Length: 438\n".getBytes());
        
        os.write("Connection: close\n".getBytes());
        
        os.write("Content-Type: ".getBytes());
        
        os.write(contentType.getBytes());
        
        os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
    }
   
    return;
    
}//end writeHTTPHeader

    
    
    
    
/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os, String loc) throws Exception
{
    loc = loc.substring(1); //location
    File f = new File(loc);
    
    //Identify whether file exists at loc or not
    //if Yes, so read file
    if(f.exists() && !f.isDirectory()){
        FileInputStream s = new FileInputStream(loc);
        BufferedReader b = new BufferedReader(new InputStreamReader(s));
        //if its format is image/
        if(format.startsWith("image/"))
            Files.copy(f.toPath(), os);
        
        else{//not image format
            String file;
            
            //countinue reading file
            while ((file = b.readLine()) != null){
                
                if(file.equals("<cs371date>")){ //if <cs371date> tag then put date
                    SimpleDateFormat dateForm = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
                    Date x = new Date();
                    String DateX = dateForm.format(x);
                    os.write(DateX.getBytes());
                }
                
                if(file.equals("<cs371server>")){ //if <cs371server> tag then message
                    os.write("This is Mohammad's Server.".getBytes());
                }
                
                os.write(file.getBytes());
            }
            
            b.close();
            
        }
        
    } else{//otherwise, display "404 Error"
        
        os.write("<h3>Error: 404 not Found</h3>".getBytes());
    }
    
}//end writeContent
    
    
} // end class
