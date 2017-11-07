package pdfp;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.io.*;
import org.apache.pdfbox.text.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.commons.io.FileUtils;
import org.apache.commons.csv.*;
import java.io.BufferedWriter; 
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class FileProcessor{
	public static final String DEFAULT_FILENAME = "Default.properties";
	public static final String PREFIX_PATTERN = "pattern.";
	public static final String PREFIX_STATIC = "static.";
	public static final String PREFIX_ETL = "etl.";
	public static final String DEFAULT_SEPARATOR = ", ";
	public static final String ESCAPE_BRACKET_START = "^\\[";
	public static final String ESCAPE_BRACKET_END = "\\]$";
	public static final String DOUBLE_PDF_EXTENSION = "\\.pdf\\.pdf$";
	public static final String PDF_EXTENSION = ".pdf";
	public static final int THRESOLD_PAGES = 6;
	
	public boolean debug;
	public String propertiesFile;
	private String patterns_prefix;
	private String patterns_separator;
	private LinkedHashMap<String, String[]> patterns;
	private String statics_prefix;
	private LinkedHashMap<String, String> statics;
	private String etl_prefix;
	private String etl_separator;
	private LinkedHashMap<String, String[]> etl;
	private int rotation_degree;
	private boolean TXT_enabled;
	private boolean TXT_append;
	private String TXT_encoding;
	private String CSV_filename;
	private String filename_entry;
	private String ETL_from;
	private String ETL_to;
	private boolean copyPDF;
	private String[] PDFformat;
	private String[] CSVformat;
	private String copyPDFsep;
	private String copyPDFETL_from;
	private String copyPDFETL_to;
	private String[] pageskip;
	private int pageskip_total;
	private String docsplit;
	private int docsplit_skip;
	private int linelimit;
	private String cache;
	private int pages;
	private int null_limit;

	public FileProcessor(){
		this(DEFAULT_FILENAME);
	}
	
	public FileProcessor(String propertiesFile){
		this(propertiesFile, false);
	}
	
	public FileProcessor(String propertiesFile, boolean writeProperties){
		this.debug = true;
		this.propertiesFile = propertiesFile;
		this.rotation_degree = 0;
		this.patterns_separator = DEFAULT_SEPARATOR;
		this.patterns_prefix = PREFIX_PATTERN;
		this.statics_prefix = PREFIX_STATIC;
		this.etl_prefix = PREFIX_ETL;
		this.etl_separator = DEFAULT_SEPARATOR;
		this.TXT_enabled = false;
		this.TXT_append = false;
		this.TXT_encoding = "UTF-8";
		this.CSV_filename = "output.csv";
		this.filename_entry = "filename";
		this.ETL_from = "\\r\\n|\\r|\\n";
		this.ETL_to = " ";
		this.copyPDF = true;
		this.copyPDFsep = " ";
		this.copyPDFETL_from = "/";
		this.copyPDFETL_to = ".";
		this.PDFformat = new String[]{"1", "2"};
		this.CSVformat = new String[]{"1", "2"};
		this.pageskip = null;
		this.pageskip_total = 0;
		this.docsplit = null;
		this.docsplit_skip = 0;
		this.linelimit = 1;
		this.cache = null;
		this.null_limit = 0;
		
		if((writeProperties == true) && (!new File(this.propertiesFile).exists())){
			this.loadDefaultPatterns();
			this.writeProperties();
		}
		this.readProperties();
		
		System.out.println(this);
	}
		
	@SuppressWarnings("unchecked")
	public void readProperties(){
		Properties prop = new Properties();
		InputStream input = null;
		try {
			prop = new Properties();
			Thread currentThread = Thread.currentThread();
			ClassLoader contextClassLoader = currentThread.getContextClassLoader();
			input = contextClassLoader.getResourceAsStream(this.propertiesFile);
			
			String classpath = System.getProperty("java.class.path");
			System.out.println("CLASSPATH: " + classpath);
			ClassLoader loader = FileProcessor.class.getClassLoader();
			System.out.println("ClassLoader resource path: " + loader.getResource(this.propertiesFile));

			if (input != null) {
				prop.load(input);

				this.rotation_degree = Integer.parseInt(prop.getProperty("rotation_degree"));
				this.debug = Boolean.parseBoolean(prop.getProperty("debug"));
				this.TXT_enabled = Boolean.parseBoolean(prop.getProperty("TXT_enabled"));
				this.TXT_append = Boolean.parseBoolean(prop.getProperty("TXT_append"));
				this.TXT_encoding = prop.getProperty("TXT_encoding");
				this.CSV_filename = prop.getProperty("CSV_filename");
				this.filename_entry = prop.getProperty("filename_entry");
				this.ETL_from = prop.getProperty("ETL_from");
				this.ETL_to = prop.getProperty("ETL_to");
				this.copyPDF = Boolean.parseBoolean(prop.getProperty("copyPDF"));
				this.copyPDFsep = prop.getProperty("copyPDFsep");
				this.copyPDFETL_from = prop.getProperty("copyPDFETL_from");
				this.copyPDFETL_to = prop.getProperty("copyPDFETL_to");
				this.patterns_separator = prop.getProperty("patterns_separator");
				String tmp = prop.getProperty("PDFformat").replaceAll(ESCAPE_BRACKET_START, "").replaceAll(ESCAPE_BRACKET_END, "");;
				this.PDFformat = tmp.split(this.patterns_separator);
				tmp = prop.getProperty("CSVformat").replaceAll(ESCAPE_BRACKET_START, "").replaceAll(ESCAPE_BRACKET_END, "");;
				this.CSVformat = tmp.split(this.patterns_separator);
				tmp = prop.getProperty("pageskip").replaceAll(ESCAPE_BRACKET_START, "").replaceAll(ESCAPE_BRACKET_END, "");;
				this.pageskip = ((tmp.equals("")) || (tmp.equals("null"))) ? null : tmp.split(this.patterns_separator);
				this.docsplit = prop.getProperty("docsplit");
				if(this.docsplit.equals("null")  || this.docsplit.equals(""))
					this.docsplit = null;
				this.docsplit_skip = Integer.parseInt(prop.getProperty("docsplit_skip"));
				this.linelimit = Integer.parseInt(prop.getProperty("linelimit"));
				this.cache = prop.getProperty("cache");
				if(this.cache.equals("null") || this.cache.equals(""))
					this.cache = null;
				this.null_limit = Integer.parseInt(prop.getProperty("null_limit"));

				this.patterns_prefix = prop.getProperty("patterns_prefix");
				this.patterns = new LinkedHashMap<String, String[]>();
			    Enumeration<String> patterns_names = (Enumeration<String>) prop.propertyNames();
			    while (patterns_names.hasMoreElements()) {
			    	String name = (String) patterns_names.nextElement();
			    	if(name.startsWith(this.patterns_prefix)){
				        String data = prop.getProperty(name).replaceAll(ESCAPE_BRACKET_START, "").replaceAll(ESCAPE_BRACKET_END, "");
				        String[] values = data.split(this.patterns_separator);
				        this.patterns.put(name.replace(this.patterns_prefix, ""), values);
			    	}
			    }
			    
			    this.statics_prefix = prop.getProperty("statics_prefix");
				this.statics = new LinkedHashMap<String, String>();
			    Enumeration<String> statics_names = (Enumeration<String>) prop.propertyNames();
			    while (statics_names.hasMoreElements()) {
			    	String name = (String) statics_names.nextElement();
			    	if(name.startsWith(this.statics_prefix)){
				        String data = prop.getProperty(name).replaceAll(ESCAPE_BRACKET_START, "").replaceAll(ESCAPE_BRACKET_END, "");
				        this.statics.put(name.replace(this.statics_prefix, ""), data);
			    	}
			    }
			    
			    this.etl_prefix = prop.getProperty("etl_prefix");
				this.etl = new LinkedHashMap<String, String[]>();
			    Enumeration<String> etl_names = (Enumeration<String>) prop.propertyNames();
			    while (etl_names.hasMoreElements()) {
			    	String name = (String) etl_names.nextElement();
			    	if(name.startsWith(this.etl_prefix)){
				        String data = prop.getProperty(name).replaceAll(ESCAPE_BRACKET_START, "").replaceAll(ESCAPE_BRACKET_END, "");
				        String[] values = data.split(this.etl_separator);
				        
				        /*
				         * TODO
						for (String value : values) {
							value = value.replaceAll(ESCAPE_BRACKET_START, "").replaceAll(ESCAPE_BRACKET_END, "");
							System.out.println("ETL: " + value);
						}
				         */
				        

				        for(int i = 0; i < values.length; i++) {
				        	String val = values[i].replaceAll(ESCAPE_BRACKET_START, "").replaceAll(ESCAPE_BRACKET_END, "");
				        	values[i] = val;
				        }
				        this.etl.put(name.replace(this.etl_prefix, ""), values);
			    	}
			    }
			    
			} else {
				System.out.println("Problem reading file: \"" + this.propertiesFile + "\"");
				System.exit(1);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	private void loadDefaultPatterns(){
		this.patterns = new LinkedHashMap<String, String[]>();
		this.patterns.put("1", (new String[]{
                               "Text1 (.*) Text2, text1 (.*) text2"
				}));
		this.patterns.put("2", (new String[]{
                               "Text3 (.*) Text4, text3 (.*) text4"
				}));
	}
	
	public void writeProperties() {
		Properties prop = new Properties();
		OutputStream output = null;
		
		try {
			output = new FileOutputStream(this.propertiesFile);

			prop.setProperty("rotation_degree", Integer.toString(this.rotation_degree));
			prop.setProperty("patterns_prefix", this.patterns_prefix);
			prop.setProperty("statics_prefix", this.statics_prefix);
			prop.setProperty("debug", Boolean.toString(this.debug));
			prop.setProperty("TXT_enabled", Boolean.toString(this.TXT_enabled));
			prop.setProperty("TXT_append", Boolean.toString(this.TXT_append));
			prop.setProperty("TXT_encoding", this.TXT_encoding);
			prop.setProperty("CSV_filename", this.CSV_filename);
			prop.setProperty("filename_entry", this.filename_entry); 
		    prop.setProperty("ETL_from", this.ETL_from);
		    prop.setProperty("ETL_to", this.ETL_to);
		    prop.setProperty("copyPDF", Boolean.toString(this.copyPDF));
		    prop.setProperty("copyPDFsep", this.copyPDFsep);
		    prop.setProperty("copyPDFETL_from", this.copyPDFETL_from);
		    prop.setProperty("copyPDFETL_to", this.copyPDFETL_to);
		    prop.setProperty("PDFformat", Arrays.toString(this.PDFformat));
		    prop.setProperty("pageskip", Arrays.toString(this.pageskip));
		    prop.setProperty("docsplit", this.docsplit);
		    prop.setProperty("docsplit_skip", Integer.toString(this.docsplit_skip));
		    prop.setProperty("linelimit", Integer.toString(this.linelimit));
		    prop.setProperty("cache", this.cache);
		    prop.setProperty("null_limit", Integer.toString(this.null_limit));
		    
		    for(Map.Entry<String, String[]> entry : this.patterns.entrySet())
			{
			    String key = entry.getKey();
			    String[] value = entry.getValue();
			    
			    prop.setProperty(this.patterns_prefix + key, Arrays.toString(value));
			}
		    for(Entry<String, String> entry : this.statics.entrySet())
			{
			    String key = entry.getKey();
			    String value = entry.getValue();
			    
			    prop.setProperty(this.statics_prefix + key, value);
			}


			prop.store(output, null);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}
	
	public String HM_toString(LinkedHashMap<String, String[]> l, String sep, String prefix) {
		StringBuilder txt = new StringBuilder();
		for(Entry<String, String[]> entry : l.entrySet())
		{
		    String key = entry.getKey();
		    
		    StringBuilder txt_values = new StringBuilder();
		    for(String value: entry.getValue()){
		    	txt_values.append(value + sep);
		    }
		    
		    txt.append(prefix + ": \"" + key + "\"\tv: \"" + txt_values + "\"" + System.lineSeparator());
		}
		return txt.toString();
	}
	
	@Override
	public String toString()
	{
		boolean enabled;
		StringBuilder txt = new StringBuilder();
		txt.append("FILE: \"" + this.propertiesFile + "\"" + System.lineSeparator());
		txt.append("F debug: " + this.debug + System.lineSeparator());
		enabled = (this.rotation_degree != 0) ? true : false;
		txt.append("F rotation: " + enabled + " - (degrees: \"" + this.rotation_degree + "\")" + System.lineSeparator());
		txt.append("F TXT: " + this.TXT_enabled + " (encoding: \"" + TXT_encoding + "\" append: \"" + this.TXT_append + "\")" + System.lineSeparator());
		txt.append("F PDF_rename: " + this.copyPDF + " (PDFformat: \"" + Arrays.toString(PDFformat) + "\" copyPDFETL_from: \"" + copyPDFETL_from + 
				"\" copyPDFETL_to: \"" + this.copyPDFETL_to + "\" copyPDFsep: \"" + copyPDFsep + "\")" + System.lineSeparator());
		enabled = (this.pageskip != null) ? true : false;
		txt.append("F pageskip: " + enabled + " - (text: \"" + Arrays.toString(this.pageskip) + "\")" + System.lineSeparator());
		enabled = (this.docsplit != null) ? true : false;
		txt.append("F multidoc: " + enabled + " - (text: \"" + this.docsplit + "\" skip: \"" + this.docsplit_skip + "\" )" + System.lineSeparator());
		enabled = (this.linelimit > 1) ? true : false;
		txt.append("F iter: " + enabled + " - \"" + this.linelimit + "\"" + System.lineSeparator());
		enabled = (this.cache != null) ? true : false;
		txt.append("F cache: " + enabled + " - \"" + this.cache + "\"" + System.lineSeparator());
		txt.append("F null_limit: \"" + this.null_limit + "\"" + System.lineSeparator());
		txt.append("PATTERNs: " + this.patterns.size() + " (prefix: \"" + this.patterns_prefix + "\" separator: \"" + this.patterns_separator + "\")" + System.lineSeparator());
		txt.append(HM_toString(this.patterns, this.patterns_separator, "p"));
		txt.append("STATICs: " + this.statics.size() + " (prefix: \"" + this.statics_prefix + "\" separator: \"" + this.patterns_separator + "\")" + System.lineSeparator());
		for(Entry<String, String> entry : this.statics.entrySet()) {
			txt.append("s: \"" + entry.getKey() + "\"\tv: " + entry.getValue() + System.lineSeparator());
		}
		txt.append("ETLs: " + this.etl.size() + " (prefix: \"" + this.etl_prefix + "\")" + System.lineSeparator());
		txt.append(HM_toString(this.etl, this.patterns_separator, "e"));
		
		return txt.toString();
	}
	
    private LinkedHashMap<String, String> pattern_match(String text, int iter){
    	Pattern p = null;
    	Matcher m = null;
    	int counter_null = 1;
    	if(this.debug)
    		System.out.println("pattern_match - iter: " + iter);
		
		LinkedHashMap<String, String> results = new LinkedHashMap<String, String>();
		if(this.patterns != null){
			
			for(Map.Entry<String, String[]> entry : this.patterns.entrySet())
			{
			    String key = entry.getKey();
			    String[] pattern = entry.getValue();
	
			    boolean data_found = false;
			    for(int i = 0; i < pattern.length; i++)
			    {
			    	if(this.debug)
			    		System.out.println("p[" + i + "] \"" + key + "\": " + pattern[i]);
	    			p = Pattern.compile(pattern[i]);
	    			m = p.matcher(text);
	    			int j;
	    			
	    			for(j = 0; j < iter && m.find(); j++);

	    			if(j > 0) {
	    				try{
	    					String found = m.group(1);
		    				if(this.debug)
		    					System.out.println("\"" + key + "\" found: \"" + found + "\" (p: " + pattern[i] + ")");
		    				found = found.replaceAll(ETL_from, ETL_to);
					    	if( this.etl.keySet().contains(key))
					    	{
					    		if(this.debug)
					    			System.out.println("ETL \"" + key + "\" src value \"" + found + "\"");
					    		String[] etls = this.etl.get(key);
					    		for(int k=0; k < etls.length; k = k + 2) {
					    			found = found.replaceAll(etls[k], etls[k+1]);
					    		}
					    		if(this.debug)
					    			System.out.println("ETL \"" + key + "\" dst value \"" + found + "\"");
					    	}
		    				results.put(key, found);
		    				data_found = true;
		    				break;
	    				}catch(IndexOutOfBoundsException | IllegalStateException e){
	    					e.printStackTrace();
	    				}
		    			if(data_found == true)
		    				//loop break at first match
		    				break;
	    			}
			    }
				if(data_found != true){
					if(this.debug)
						System.out.println("Nothing found for \"" + key + "\"");
					results.put(key, null);
					counter_null++;
				}
				if(counter_null >= this.null_limit) {
			    	if(this.debug)
			    		System.out.println("NULLCounter: " + counter_null + "/" + this.null_limit);
			    	return results;
				}
			}
			//Add static fields
			for(Map.Entry<String, String> entry : this.statics.entrySet()) {
				results.put(entry.getKey(), entry.getValue());
			}
		}
		return results;
    }
    
    public String[] split(String parsedText) {
    	String[] result;
    	if(this.docsplit != null && this.pages > THRESOLD_PAGES) {
	    	System.out.println("MULTIDOC Split by \"" + this.docsplit + "\"");
	    	String[] splitted = parsedText.split(this.docsplit);
	    	result = Arrays.copyOfRange(splitted, this.docsplit_skip, splitted.length);
    	} else
    		result = new String[] {parsedText};
    	return result;
    }
	
    public boolean match(File[] inputFiles, String folderDest) {
		System.out.println("Arguments");
		System.out.println("File[] inputFiles: " + Arrays.toString(inputFiles));
		System.out.println("String folderDest: " + folderDest);
		System.out.println("-----------------");
		
        if (!new File(folderDest).exists())
        {
            new File(folderDest).mkdir();
        }
		
        long time;
        for (int index = 0; index < inputFiles.length; index++) {
    		this.pages = 0;
            System.out.println("Conversion to text from file \"" + inputFiles[index] + "\"");
            time = System.currentTimeMillis();
            
            String pdfFile = inputFiles[index].getName();
            
            String parsedText = this.getTextFromPDF(inputFiles[index], this.rotation_degree, folderDest);
            
            String[] docs = this.split(parsedText);
            System.out.println("Number of doc(s): " + docs.length);
            
            List<String> header = null;
            String PDF_rename = inputFiles[index].getName();
            ArrayList<String[]> results = new ArrayList<String[]>();
            LinkedHashMap<String, String> result = null;

        	if (this.TXT_enabled){
        		System.out.println("Write to TXT");
            	String textFile = folderDest + "\\" + inputFiles[index].getName().substring(0, pdfFile.length() -3 ) + "txt";
        		this.writeTXT(new File(textFile), parsedText, this.TXT_encoding, this.TXT_append);
        		if (this.debug)
        			System.out.println("TXT created in " + ((System.currentTimeMillis() - time) / 1000.0) + " s");
        	}
            
        	String cache_value = null;
            for(String doc: docs) {
	            if(doc != null) {
	                for(int iter = 1; iter < this.linelimit; iter++) {
		                result = this.pattern_match(doc, iter);
		                if(result.size() > this.null_limit){
		                	if(this.cache != null) {
			                	String cache_current = result.get(this.cache);
			                	if(cache_current == null || cache_current.equals("") || cache_current.equals("null")) {
			                		System.out.println("GET cache \""+ this.cache +"\" = " + cache_value + " (read \"" + cache_current + "\")");
			                		result.put(this.cache, cache_value);
			                	} else {
			                		System.out.println("SET cache \""+ this.cache +"\" = " + cache_current);
			                		cache_value = cache_current;
			                	}
		                	}//cache feature
			                PDF_rename = this.get_namePDF(inputFiles[index], folderDest, result);
						    result.put(this.filename_entry, PDF_rename);
			                System.out.println("---------------------------------------------------------------------");
			                
			        		header = this.getResultHeader(result);
			        		String[] record = this.getResultData(result);
			        		results.add(record);
			        		
		        			for(int j=0; j < this.CSVformat.length; j++) {
		        				System.out.println("k: " + this.CSVformat[j] + "\t\tv: " + record[j]);
			        		}
		                }
	                }
	            }
            }
            File PDFsoure = inputFiles[index];
            if(this.copyPDF)
            	PDFsoure = new File(new File(folderDest), inputFiles[index].getName());
            this.renamePDF(PDFsoure, new File(new File(folderDest), PDF_rename));
            
            String csvFile = folderDest + "\\" + this.CSV_filename;
            this.writeCSV(csvFile, header, results);
			
            System.out.println("PAGEs SKIPPED: " + this.pageskip_total);
            System.out.println("PDF file processed in: " + ((System.currentTimeMillis() - time) / 1000.0) + " s");
        }
        return true;
    } //match

    public static int[] convertIntegers(List<Integer> integers)
    {
        int[] ret = new int[integers.size()];
        for (int i=0; i < ret.length; i++)
        {
            ret[i] = integers.get(i).intValue();
        }
        return ret;
    }
    
    private int[] calcPagRemove(PDDocument pdDoc) {
    	ArrayList<Integer> result = new ArrayList<Integer>();
    	
    	try {
	    	PDFTextStripper stripper = new PDFTextStripper();
	    	String txt;
	    	boolean data_found;
	        for (int i=1; i <= pdDoc.getNumberOfPages(); i++){
	        	data_found = false;
	        	
	        	stripper.setStartPage(i);
	        	stripper.setEndPage(i);
	        	txt = stripper.getText(pdDoc);

	        	if(txt == null || txt.equals("") || txt.hashCode() == 0) {
	        		if(this.debug)
	        			System.out.println("Found empty page: " + i);
	        		if(!result.contains(i))
	        			result.add((i));
	        		data_found = true;
	        	}
	        	
	        	for(int j=0; j < this.pageskip.length; j++) {
			    	if(this.debug)
			    		System.out.println("s[" + i + "] :\"" + this.pageskip[j] + "\"");
	        		Pattern p = Pattern.compile(this.pageskip[j]);
	    			Matcher m = p.matcher(txt);
	    			
	    			while (m.find()) {
	    				if(this.debug)
	    					System.out.println("\"pageskip\" found: \"" + m.group() + "\" (p: " + this.pageskip[j] + ")");
	    				try{
	    	        		if(!result.contains(i))
	    	        			result.add((i));
		    				data_found = true;
		    				break;
	    				}catch(IndexOutOfBoundsException e){
	    					e.printStackTrace();
	    				}
	    			}
	    			if(data_found == true)
	    				//loop break at first match
	    				break;
	        	}
	        }
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	return FileProcessor.convertIntegers(result);
    }
		
    private String getTextFromPDF(File pf, int rotation, String folderDest){
    	String parsedText = null;
	    try {
	    	PDDocument pdDoc = PDDocument.load(pf);
	        if (pdDoc.isEncrypted()) {
	        	System.out.println("The document is encrypted");
	        	pdDoc.setAllSecurityToBeRemoved(true);
	        	if (pdDoc.isAllSecurityToBeRemoved()) {
	            	System.out.println("We DECRYPTED it!");
	            }else {
	        		System.out.println("We can't decrypt it.");
	            }
	        }
	        this.pages = pdDoc.getNumberOfPages();
	    	if(rotation != 0){
		        PDPageTree pages = pdDoc.getDocumentCatalog().getPages();

		        for (PDPage page: pages)
		        {
		        	page.setRotation(rotation);
		        }
	    	}
	        parsedText = new PDFTextStripper().getText(pdDoc);
	        if(this.pageskip != null) {
	        	int src_pages = pdDoc.getNumberOfPages();
	        	int[] removepages = this.calcPagRemove(pdDoc);
	        	if(removepages != null) {
	        		for(int i=(removepages.length - 1); i >= 0; i--) {
        				pdDoc.removePage((removepages[i] - 1));
	        		}
    	        	int dst_pages = pdDoc.getNumberOfPages();
    	        	this.pageskip_total += src_pages - dst_pages; 
		        	System.out.println("Number of pages: SRC=" + src_pages + " DST=" + dst_pages + " (removed pages: " + Arrays.toString(removepages) + ")");
	        	}	        	
	        }
	        if(this.copyPDF){
	        	pdDoc.save(new File(new File(folderDest), pf.getName()));
	        }
	        pdDoc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	    return parsedText;
    }
    
    private void writeTXT(File output, String text, String encoding, boolean append){
    	try{
    		FileUtils.writeStringToFile(output, text, encoding, append);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private List<String> getResultHeader(LinkedHashMap<String, String> results){
		return new ArrayList<String>(Arrays.asList((this.CSVformat)));
	}
    
    private String[] getResultData(LinkedHashMap<String, String> results){
    	List<String> list_values = new ArrayList<String>();
    	
    	for(int i=0; i < this.CSVformat.length; i++) {
    		try {
    			String value = results.get(this.CSVformat[i]);
    			list_values.add(value);
    		}catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
   		String[] values = new String[list_values.size()];
    	values = list_values.toArray(values);
    	return values;
    }
    
    private void writeCSV(String filename, List<String> header, ArrayList<String[]> values){
    	try{
    		boolean file_exists = new File(filename).exists();
    		StandardOpenOption[] opts = {StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND};
	    	BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename), StandardCharsets.UTF_8, opts);
	    	CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL);
	    	
			if(!file_exists && header != null){
	            for (String head : header) {
	                printer.print(head);
	            }
	            printer.println();
			}//header
			
            for (String[] line : values) {
            	for (String val: line) {
					printer.print(val);
            	}
            	printer.println();
            }
            
			printer.flush();
			printer.close();
			System.out.println("Written to \""+ filename + "\"");
			
        } catch (IOException e) {
            e.printStackTrace();
        }   	
    } //writeCSV
    
    private String get_namePDF(File pf, String folderDest, LinkedHashMap<String, String> result){
		String base_dest_filename = "";
		for (String i: this.PDFformat){
			String val = result.get(i);
			if(val == null || val.equals("null")) {
				base_dest_filename = pf.getName();
				break;
			} else {
				base_dest_filename += val + this.copyPDFsep;
			}
		}
		base_dest_filename = base_dest_filename.trim();
		
		int i = 1;
		File file_dest;
		String dest_filename;
		do{
			if(i == 1)
				dest_filename = (base_dest_filename + PDF_EXTENSION);
			else
				dest_filename = (base_dest_filename + this.copyPDFsep + i + PDF_EXTENSION);
			dest_filename = dest_filename.replaceAll(this.copyPDFETL_from, this.copyPDFETL_to);
			file_dest = new File(new File(folderDest), dest_filename);
			i++;
		}while(file_dest.exists());
		String output = file_dest.getName();
		output = output.replaceAll(DOUBLE_PDF_EXTENSION, PDF_EXTENSION);
		System.out.println("PDF rename CALC: " + output);
		return output;
    }
    
    private String renamePDF(File source, File destination){
		if(source.renameTo(destination))
			System.out.println("PDF moved to \""+ destination + "\"");
		else
			System.out.println("PDF rename error to \""+ destination + "\"");
		return destination.getName();
    }
    
	public static String getDate()
	{
		String DEFAULT_TIMEZIONE = "Europe/Rome";
		TimeZone tz = TimeZone.getTimeZone(DEFAULT_TIMEZIONE);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		return df.format(new Date()); 
	}
    
    public static void main(String[] args){
		String DateStart = FileProcessor.getDate();
		if(args.length < 3){
			System.out.println("Usage: PROPERTYFILE OUTPUTDIR INPUTFILE1/INPUTDIR1 ... INPUTFILEn//INPUTDIRn");
		}else{
			FileProcessor p = new FileProcessor(args[0]);
			System.out.println("Properties: " + args[0]);
			System.out.println("OUT: " + args[1]);
			String[] inputs = Arrays.copyOfRange(args, 2, args.length);
			System.out.println("IN:  " + Arrays.toString(inputs));
			p.match(inputs, args[1]);
		}
		System.out.println("START: " + DateStart);
		System.out.println("END:   " + FileProcessor.getDate());
    }

	private void match(String[] files, String folderDest) {
		List<File> f = new ArrayList<File>();
		File f1;
		for(int i = 0; i < files.length; i++){
			f1 = new File(files[i]);
			if(f1.isDirectory()){
				for (File item: f1.listFiles()){
					f.add(item);
				}
			} else {
				f.add(f1);
			}
		}
		File[] af = new File[f.size()];
		af = f.toArray(af);
		this.match(af, folderDest);
	}

	
}//class
