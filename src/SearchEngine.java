import java.io.File;

import java.io.FileWriter;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.util.HashMap;
import java.util.HashSet;


import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class SearchEngine {

	final private static boolean PRINT_INDEX_TO_SCREEN = false;
	final private static boolean PRINT_INDEX_TO_FILE = false;
	final private static boolean PRINT_METRIC_TO_SCREEN = true;
	final private static boolean PRINT_METRIC_TO_FILE = false;
	
	final private static boolean GET_CONTENT_URL = false;
	final private static boolean PRINT_CONTENT_STRING = false;
	final private static boolean PRINT_CONTENT_BODY = false;
	final private static boolean PRINT_CONTENT_TEXT = false;

	final private static boolean USE_REAL_FILES = true;
	final private static int REAL_FILE_INDEX_LIMIT = -1;
	
	final private static String REAL_INDEX = "index";
	final private static String HUMAN_READABLE_INDEX = "index.txt";
	
	final private static String INDEX_METRIC_SIZE_KEY = "indexSize";
	final private static String INDEX_METRIC_SIZE_COUNT_KEY = "indexFileCount";
	final private static String INDEX_METRIC_UNIQUE_KEY = "uniqueKeyCount";
	final private static String INDEX_METRIC_DOC_CT_KEY = "numberOfDocsRead";
	final private static String INDEX_METRIC_UNPARSABLE_CT = "numberOfUnparsableFiles";
	
	
	public static void main(String[] args) throws IOException, ParseException{
		 StandardAnalyzer analyzer = new StandardAnalyzer(); 
		 		
		 File indexFile = new File(REAL_INDEX);
		 
		 //Check to make sure the index directory exists
		 if(!indexFile.isDirectory())
		 {
			indexFile.mkdir(); 
		 }
		 
		 Directory index = new SimpleFSDirectory(indexFile.toPath());																						
		 IndexWriterConfig config = new IndexWriterConfig(analyzer);
		 
		 //Sets how we handles an existing index
		 config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		 IndexWriter w = new IndexWriter(index, config);				
		 
		 //URL path = Test_Lucene.class.getResource("SampleTextDoc.txt"); //How to get txt that is in same directory to avoid complications
		 File bookKeeping = new File("WEBPAGES_RAW/bookkeeping.json"); 
		 JSONObject jsonObj = new JSONObject(String.join("", Files.readAllLines(bookKeeping.toPath(), StandardCharsets.UTF_8)));
		 
		 File inputFile = null;
		 
		 int numberOfUnparsableFiles = 0;
		 //THIS CHECK IS ONLY FOR DEVELOPMENT
		 if(USE_REAL_FILES)
		 {
			 JSONArray nameArr = jsonObj.names();
			 
			 // Traverse our bookeeping JSON file that has all of the paths of the files for us to index
			 for(int i = 0; i < nameArr.length() && (i < REAL_FILE_INDEX_LIMIT || REAL_FILE_INDEX_LIMIT == -1); i++)
			 {
				 System.out.println("\nCurrently Parsing #" + (i + 1) + " : WEBPAGES_RAW/" + (String)nameArr.get(i) + (GET_CONTENT_URL ? " -- This is the URL: " + jsonObj.getString((String)nameArr.get(i)) : ""));
				 
				 inputFile = new File("WEBPAGES_RAW/" + (String)nameArr.get(i));
				 
				 try{
					 
					 if(addDoc(w, jsonObj.getString((String)nameArr.get(i)), inputFile) == -1)
					 {
						 numberOfUnparsableFiles++;
					 }
				 }catch(IllegalArgumentException e)
				 {
					 System.out.println("***ILLEGAL ARGUMENTS FOUND***: " + e.getMessage());
					 numberOfUnparsableFiles++;
				 }
			 }
		 }
		 else
		 {
			 //***TEST CODE***
//			 inputFile = new File("SampleTextDoc.txt"); 
//			 addDoc(w, "www1", inputFile);
//			 
//			 inputFile = new File("secondSampleTextDoc.txt"); 
//			 addDoc(w, "www2", inputFile);
			 
			 inputFile = new File("WEBPAGES_RAW/0/189"); 
			 addDoc(w, "www2", inputFile);
		 }

		 
		//Close or commit IndexWriter to push changes for IndexReader
		 w.close();
		 
		 
		 System.out.println("***INDEXING COMPLETE***");
		 
		 //Creating our index
		 IndexReader reader = DirectoryReader.open(index);
		 
		 HashMap<String, HashSet<String>> hmap = getIndexAsMap(reader);
		 printOutIndex(hmap);
		 
		 HashMap<String, String> metrics = getMetrics(hmap, reader); 
		 
		 //add the bad file metric... should probably just make this map global
		 metrics.put(INDEX_METRIC_UNPARSABLE_CT, String.valueOf(numberOfUnparsableFiles));
		 printMetrics(metrics);

		 reader.close();    
		 
	}
	
	
	//Will add new document to Index
	private static int addDoc(IndexWriter w, String url, File file) throws IOException, IllegalArgumentException {
		
		//TODO: needs to be able to parse HTML pages here
		//File parsing
		org.jsoup.nodes.Document html = Jsoup.parse(String.join("",Files.readAllLines(file.toPath())));
		
		//If we cant parse the html
		if(html == null)
			return -1;
		
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
		
		//Need to make sure we have content before attempting to add a link to a document
		if(doc.getFields().size() > 0)
		{
			//fileID field should not be used for finding terms within document, only for uniquely identifying this doc amoungst others in index
			type = new FieldType();
			type.setStored(true);
			type.setTokenized(false);
			type.setStoreTermVectors(false);
			doc.add(new Field("url", url, type));
			
			w.addDocument(doc);
			
			return 0;
		}

		
		return -1;
	}
	
	
	/*
	Will iterate through all documents held within the index and append to a map with key representing the term and value being all documents that have
	the term within it. This will assume terms can be within any field even the title of the document
	*/
	private static HashMap<String, HashSet<String>> getIndexAsMap(IndexReader reader) throws IOException{
		 HashMap<String, HashSet<String>> hmap = new HashMap<String, HashSet<String>>();
		 HashSet<String> docIdSet;
		 
		 for(int i=0; i<reader.numDocs(); i++){
			 Fields termVect = reader.getTermVectors(i);
		
			 if(termVect == null)
				 continue;
			 
	         for (String field : termVect) {
	        	 
	             Terms terms = termVect.terms(field);
	             TermsEnum termsEnum = terms.iterator();
	             Document doc = reader.document(i);
	             
	             while(termsEnum.next() != null){
	            	 BytesRef term = termsEnum.term();
	            	 String strTerm = term.utf8ToString();
	            	 
	            	 if (hmap.containsKey(strTerm)){
	            		 docIdSet = hmap.get(strTerm);
	            		 docIdSet.add(doc.get("url"));
	            	 } else{
	            		 docIdSet = new HashSet<String>();
	            		 docIdSet.add(doc.get("url"));
	            		 hmap.put(strTerm, docIdSet);
	            	 }
	             }
	         }
		 }
		 
		 return hmap;
	}
	
	private static HashMap<String, String> getMetrics(HashMap<String, HashSet<String>> hmap, IndexReader reader)
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
		 
		 //Puts the metric results in a hashmap
		HashMap<String, String> results = new HashMap<>(); 
		results.put(INDEX_METRIC_SIZE_KEY, String.format("%.2f", totalIndexSize));
		results.put(INDEX_METRIC_SIZE_COUNT_KEY, String.valueOf(numOfCfsFiles));
		results.put(INDEX_METRIC_UNIQUE_KEY, String.valueOf(hmap.size()));
		results.put(INDEX_METRIC_DOC_CT_KEY, String.valueOf(reader.numDocs()));

		return results;
	}
	
	//Iterates through map and prints out as a postings as seen in lectures
	private static void printOutIndex(HashMap<String, HashSet<String>> hmap){
		
		try{
			//False overwrites old data
			FileWriter writer = new FileWriter(HUMAN_READABLE_INDEX, false); 
		
			//creating output string
			String output = new String();
	        for(String key: hmap.keySet()){
		       	output = key + " -> " + String.join(", ", hmap.get(key));
		       	
		        //Printing to screen
		        if(PRINT_INDEX_TO_SCREEN)
		        	System.out.println(output);
		        
		        if(PRINT_INDEX_TO_FILE){
					try {
						writer.write(output + "\n");
					} catch (IOException e){}	
		        }
		       	
	        }
	        
	        writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void printMetrics(HashMap<String, String> metrics){
		if(PRINT_METRIC_TO_SCREEN)
		{
			System.out.println("\nTotal number of flat (.cfs files) files storing the index: " + metrics.get(INDEX_METRIC_SIZE_COUNT_KEY));
			System.out.println("Size of the complete index size: " + metrics.get(INDEX_METRIC_SIZE_KEY) +  " MB");
			System.out.println("Total Unique Terms: " + metrics.get(INDEX_METRIC_UNIQUE_KEY));
			System.out.println("Total number of documents: " + metrics.get(INDEX_METRIC_DOC_CT_KEY));
			System.out.println("Total number of unparsable documents: " + metrics.get(INDEX_METRIC_UNPARSABLE_CT) );

		}
	
		if(PRINT_METRIC_TO_FILE)
		{
			try 
			{
				FileWriter writer = new FileWriter(HUMAN_READABLE_INDEX, true); //the true will append the new data
				writer.write("\n");
				writer.write("Total number of flat files (.cfs files) storing the index: " + metrics.get(INDEX_METRIC_SIZE_COUNT_KEY) + "\n");
				writer.write("Size of the complete index size: " + metrics.get(INDEX_METRIC_SIZE_KEY) + " MB\n");
				writer.write("Total Unique Terms: " + metrics.get(INDEX_METRIC_UNIQUE_KEY) + "\n");
				writer.write("Total number of documents encountered: " + metrics.get(INDEX_METRIC_DOC_CT_KEY) + "\n");
				writer.write("Total number of unparsable documents: " + metrics.get(INDEX_METRIC_UNPARSABLE_CT) + "\n");
				
				writer.close();
			} 
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

}
