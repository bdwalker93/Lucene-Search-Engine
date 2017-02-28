import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
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
import org.apache.lucene.util.BytesRef;


public class Test_Lucene {

	public static void main(String[] args) throws IOException, ParseException{
		 StandardAnalyzer analyzer = new StandardAnalyzer(); 			
		 Directory index = new RAMDirectory();																						
		 IndexWriterConfig config = new IndexWriterConfig(analyzer);	
		 IndexWriter w = new IndexWriter(index, config);				
		 
		 //URL path = Test_Lucene.class.getResource("SampleTextDoc.txt"); //How to get txt that is in same directory to avoid complications
		 
		 //TODO:Instead of this being a single file, we will recurse through the files of a root directory
		 File file = new File("SampleTextDoc.txt"); 
		 addDoc(w, file);
		 
		//Close or commit IndexWriter to push changes for IndexReader
		 w.close();	
		 
		 //Creating our index
		 IndexReader reader = DirectoryReader.open(index);
		 
		 HashMap<String, ArrayList<String>> hmap = getIndexAsMap(reader);
		 printOutIndex(hmap);
         
		 reader.close();    
		 
		 
	}
	
	
	//Will add new document to Index
	private static void addDoc(IndexWriter w, File file) throws IOException {
		
		//TODO: needs to be able to parse HTML pages here
		//File parsing
		List<String> lines = Files.readAllLines(Paths.get(file.getPath()));
		String content = String.join(", ", lines);
		String title = file.getName();
		
		//Document Creation
		Document doc = new Document();
		FieldType type = new FieldType();
		type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
		type.setStored(true); 
		type.setStoreTermVectors(true);
		type.setTokenized(true);
		  
		doc.add(new Field("title", title, type));
		doc.add(new Field("content", content, type));
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
	            		 list.add(doc.get("title"));
	            	 } else{
	            		 list = new ArrayList<String>();
	            		 list.add(doc.get("title"));
	            		 hmap.put(strTerm, list);
	            	 }
	             }
	         }
		 }
		 
		 return hmap;
	}
	
	//Iterates through map and prints out as a postings as seen in lectures
	private static void printOutIndex(HashMap<String, ArrayList<String>> hmap){
		String outputFileName = "index.txt";
		
		
		String output = new String();
        for(String key: hmap.keySet()){
	       	output += key + " -> ";
	       	String result = String.join(", ", hmap.get(key));
	       	output += result;
	       	output += "\n";
        }
        
       	print(output);
        
        //This is so ugly!!!!!
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFileName), "utf-8"))) {
        	writer.write(output);
        }catch(Exception e){}
	}
	
	//For my personal use to mimic print from python
	private static void print(Object object){
		System.out.println(object);
	}

}
