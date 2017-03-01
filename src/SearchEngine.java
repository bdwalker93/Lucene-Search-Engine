import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;


public class SearchEngine {

	final private static boolean PRINT_INDEX_TO_SCREEN = true;
	final private static boolean PRINT_INDEX_TO_FILE = false;
	
	final private static boolean GET_CONTENT_URL = false;
	final private static boolean PRINT_CONTENT_STRING = false;
	final private static boolean PRINT_CONTENT_BODY = false;
	final private static boolean PRINT_CONTENT_TEXT = true;

	final private static boolean USE_REAL_FILES = true;
	final private static int REAL_FILE_INDEX_LIMIT = -1;
	
	final private static String REAL_INDEX = "index";
	final private static String HUMAN_READABLE_INDEX = "index.txt";
	
	public static void main(String[] args) throws IOException, ParseException{
		 StandardAnalyzer analyzer = new StandardAnalyzer(); 
		 
		 //TODO: This is not a persistant index. We need to find something that stores the index. http://stackoverflow.com/questions/9146604/how-to-persist-the-lucene-document-index-so-that-the-documents-do-not-need-to-be
		getMetrics(); 
//		 File indexFile = new File(REAL_INDEX);
//		 
//		 //Check to make sure the index directory exists
//		 if(!indexFile.isDirectory())
//		 {
//			indexFile.mkdir(); 
//		 }
//		 
//		 Directory index = new SimpleFSDirectory(indexFile.toPath());																						
//		 IndexWriterConfig config = new IndexWriterConfig(analyzer);
//		 
//		 //Sets how we handles an existing index
//		 config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
//		 IndexWriter w = new IndexWriter(index, config);				
//		 
//		 //URL path = Test_Lucene.class.getResource("SampleTextDoc.txt"); //How to get txt that is in same directory to avoid complications
//		 File bookKeeping = new File("WEBPAGES_RAW/bookkeeping.json"); 
//		 JSONObject jsonObj = new JSONObject(String.join("", Files.readAllLines(bookKeeping.toPath(), StandardCharsets.UTF_8)));
//		 
//		 File inputFile = null;
//		 
//		 //THIS CHECK IS ONLY FOR DEVELOPMENT
//		 if(USE_REAL_FILES)
//		 {
//			 JSONArray nameArr = jsonObj.names();
//			 
//			 // Traverse our bookeeping JSON file that has all of the paths of the files for us to index
//			 for(int i = 0; i < nameArr.length() && i < REAL_FILE_INDEX_LIMIT || REAL_FILE_INDEX_LIMIT == -1; i++)
//			 {
//				 System.out.println("\nCurrently Parsing #" + i + " : WEBPAGES_RAW/" + (String)nameArr.get(i) + (GET_CONTENT_URL ? " -- This is the URL: " + jsonObj.getString((String)nameArr.get(i)) : ""));
//				 
//				 inputFile = new File("WEBPAGES_RAW/" + (String)nameArr.get(i));
//				 
//				 try{
//				 addDoc(w, inputFile);
//				 }catch(IllegalArgumentException e)
//				 {
//					 System.out.println("***ILLEGAL ARGUMENTS FOUND***: " + e.getMessage());
//				 }
//			 }
//		 }
//		 else
//		 {
//			 //***TEST CODE***
//			 inputFile = new File("SampleTextDoc.txt"); 
//			 addDoc(w, inputFile);
//			 
//			 inputFile = new File("secondSampleTextDoc.txt"); 
//			 addDoc(w, inputFile);
//		 }
//
//		 
//		//Close or commit IndexWriter to push changes for IndexReader
//		 w.close();	
//		 
//		 //Creating our index
//		 IndexReader reader = DirectoryReader.open(index);
//		 
//		 HashMap<String, ArrayList<String>> hmap = getIndexAsMap(reader);
//		 printOutIndex(hmap);
//         
//		 reader.close();    
		 
	}
	
	
	//Will add new document to Index
	private static void addDoc(IndexWriter w, File file) throws IOException, IllegalArgumentException {
		
		//TODO: needs to be able to parse HTML pages here
		//File parsing
		org.jsoup.nodes.Document html = Jsoup.parse(String.join("",Files.readAllLines(file.toPath())));
		
		//If we cant parse the html
		if(html == null)
			return;
		
		if(PRINT_CONTENT_STRING)
			System.out.println("***This is the body***\n" + String.join("",Files.readAllLines(file.toPath())));
		
		if(PRINT_CONTENT_BODY)
			System.out.println("***This is the body***\n" + html.body());
		
		String content = null;
		Element body = html.body();
		
		if(body != null)
			content = body.text();
		
		if(PRINT_CONTENT_TEXT)
			System.out.println("***This is the BODY text***\n" + content);

		String title = null;
		Element head = html.head();
		if(head != null)
			title = head.text();
		
		if(PRINT_CONTENT_TEXT)
			System.out.println("***This is the TITLE***\n" + title);
				
		//Document Creation
		Document doc = new Document();
		FieldType type = new FieldType();
		type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
		type.setStored(true); 
		type.setStoreTermVectors(true);
		type.setTokenized(true);
		
		//If there is text in the head, it is probably a title
		if(title != null && !title.isEmpty())
			doc.add(new Field("title", title, type));
		
		//Need to make sure there is a body and it isnt empty
		if(content != null && !content.isEmpty())
			doc.add(new Field("content", content, type));
		
		doc.add(new Field("fileId", file.getName(), type));
		
		w.addDocument(doc);
	}
	
	
	/*
	Will iterate through all documents held within the index and append to a map with key representing the term and value being all documents that have
	the term within it. This will assume terms can be within any field even the title of the document
	*/
	private static HashMap<String, ArrayList<String>> getIndexAsMap(IndexReader reader) throws IOException{
		 HashMap<String, ArrayList<String>> hmap = new HashMap<String, ArrayList<String>>();
		 ArrayList<String> list;
		 
		 for(int i=0; i<reader.numDocs(); i++){
			 Fields termVect = reader.getTermVectors(i);
			 
	         for (String field : termVect) {
	        	 
	             Terms terms = termVect.terms(field);
	             TermsEnum termsEnum = terms.iterator();
	             Document doc = reader.document(i);
	             
	             while(termsEnum.next() != null){
	            	 BytesRef term = termsEnum.term();
	            	 String strTerm = term.utf8ToString();
	            	 
	            	 if (hmap.containsKey(strTerm)){
	            		 list = hmap.get(strTerm);
	            		 list.add(doc.get("fileId"));
	            	 } else{
	            		 list = new ArrayList<String>();
	            		 list.add(doc.get("fileId"));
	            		 hmap.put(strTerm, list);
	            	 }
	             }
	         }
		 }
		 
		 return hmap;
	}
	
	private static void getMetrics()
	{
		double totalIndexSize = 0;
		int numOfCfsFiles = 0;
		
		File indexFile = new File(REAL_INDEX);
		 	
		 for(File file : indexFile.listFiles())
		 {
			 if(file.getName().endsWith(".cfs"))
			 {
				 numOfCfsFiles++;
				 totalIndexSize += file.length() / (double)1024 / 1024;
			 }
		 }
		
		 System.out.println("Total index ct: " + numOfCfsFiles + " -- Total index size: " + totalIndexSize);
	}
	
	//Iterates through map and prints out as a postings as seen in lectures
	private static void printOutIndex(HashMap<String, ArrayList<String>> hmap){
		
		//creating output string
		String output = new String();
        for(String key: hmap.keySet()){
	       	output += key + " -> ";
	       	String result = String.join(", ", hmap.get(key));
	       	output += result;
	       	output += "\n";
        }
        
        //Printing to screen
        if(PRINT_INDEX_TO_SCREEN)
       	System.out.println(output);
       	
       	//Prints index to a file (based on the print boolean control)
       	if(PRINT_INDEX_TO_FILE){
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(HUMAN_READABLE_INDEX), "utf-8"))) {
            	writer.write(output);
            }catch(Exception e){}
       	}

	}

}
