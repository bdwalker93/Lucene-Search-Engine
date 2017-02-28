/*
 * Various parts adapted from https://examples.javacodegeeks.com/core-java/apache/lucene/lucene-indexing-example/
 * */

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class SearchEngine {

	public static void main(String[] args) throws IOException {

		StandardAnalyzer analyzer = new StandardAnalyzer();

		Directory index = new RAMDirectory();
		//Directory index = FSDirectory.open(new File("index-dir"));
		IndexWriterConfig config = new IndexWriterConfig( analyzer);
		IndexWriter writer = new IndexWriter(index, config);
	}

	public Document getDocument(File file){
		
		Document document = new Document();
		
		document.add(new TextField(LuceneConstants.FILE_NAME, file.getName(), Field.Store.YES));
		document.add(new TextField(LuceneConstants.FILE_PATH, file.getName(), Field.Store.YES));
		document.add(new TextField(LuceneConstants.CONTENTS, "THIS IS SOME CONTENT", Field.Store.YES));
//		document.add(new StringField("course_code", courseCode, Field.Store.YES));

		return document;
	}
}
