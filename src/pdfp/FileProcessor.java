package pdfp;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.io.*;
import org.apache.pdfbox.text.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.commons.io.FileUtils;
import org.apache.commons.csv.*;
import java.io.BufferedWriter; 
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileProcessor{
	public static final String DEFAULT_FILENAME = "Default.properties";
	public static final String PREFIX_PATTERN = "pattern.";
	
	public boolean debug;
	public String propertiesFile;
	private String patterns_prefix;
	private String patterns_separator;
	private int rotation_degree;
	private boolean TXT_enabled;
	private boolean TXT_append;
	private String TXT_encoding;
	private String CSV_filename;
	private String filename_entry;
	private String ETL_from;
	private String ETL_to;
	public boolean copyPDF;
	public String[] PDFformat;
	public String copyPDFsep;
	public String copyPDFETL_from;
	public String copyPDFETL_to;
	private HashMap<String, String[]> patterns;

	public FileProcessor(){
		this(DEFAULT_FILENAME);
	}
	
	public FileProcessor(String propertiesFile){
		this.debug = true;
		this.propertiesFile = propertiesFile;
		this.patterns_separator = ", ";
		this.rotation_degree = 0;
		this.patterns_prefix = PREFIX_PATTERN;
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
		
		if(!new File(this.propertiesFile).exists()){
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
				this.patterns_prefix = prop.getProperty("patterns_prefix");
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
				String tmp = prop.getProperty("PDFformat").replaceAll("^\\[", "").replaceAll("\\]$", "");;
				this.PDFformat = tmp.split(this.patterns_separator);
				

				this.patterns = new HashMap<String, String[]>();
			    Enumeration<String> names = (Enumeration<String>) prop.propertyNames();
			    while (names.hasMoreElements()) {
			    	String name = (String) names.nextElement();
			    	if(name.startsWith(this.patterns_prefix)){
				        String data = prop.getProperty(name).replaceAll("^\\[", "").replaceAll("\\]$", "");
				        String[] values = data.split(this.patterns_separator);
				        this.patterns.put(name.replace(this.patterns_prefix, ""), values);
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
		this.patterns = new HashMap<String, String[]>();
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
		    
		    for(Map.Entry<String, String[]> entry : this.patterns.entrySet())
			{
			    String key = entry.getKey();
			    String[] value = entry.getValue();
			    
			    prop.setProperty("pattern." + key, Arrays.toString(value));
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
	
	@Override
	public String toString(){
		String txt = "File: " + this.propertiesFile + System.lineSeparator();
		for(Entry<String, String[]> entry : this.patterns.entrySet())
		{
		    String key = entry.getKey();
		    String[] values = entry.getValue();
		    
		    StringBuilder txt_values = new StringBuilder();
		    for(String value: values){
		    	txt_values.append(value + this.patterns_separator);
		    }
		    
		    txt += "p: \"" + key + "\"\tv: " + txt_values + System.lineSeparator();
		}
		return txt;
	}
			
    private HashMap<String, String> pattern_match(String pdffile, String text){    	     	
    	Pattern p = null;
    	Matcher m = null;
		System.out.println("ready for the pattern_match!");
		
		HashMap<String, String> results = new HashMap<String, String>();
		if(this.patterns != null){
			for(Map.Entry<String, String[]> entry : this.patterns.entrySet())
			{
			    String key = entry.getKey();
			    String[] pattern = entry.getValue();
	
			    boolean data_found = false;
			    results.put(this.filename_entry, pdffile);
			    for(int i = 0; i < pattern.length; i++)
			    {
			    	if(this.debug)
			    		System.out.println("p[" + i + "] \"" + key + "\": " + pattern[i]);
	    			p = Pattern.compile(pattern[i]);
	    			m = p.matcher(text);
	    			
	    			while (m.find()) {
	    				if(this.debug)
	    					System.out.println("\"" + key + "\" found: \"" + m.group() + "\" (p: " + pattern[i] + ")");
	    				try{
	    					String found = m.group(1);
		    				found = found.replaceAll(ETL_from, ETL_to);
		    				results.put(key, found);
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
				if(data_found != true){
					if(this.debug)
						System.out.println("Nothing found for \"" + key + "\"");
					results.put(key, null);
				}
			    
			}
		}
		return results;
    }
	
    public boolean match(File[] inputFiles, String PDFfilenames, String folderDest) {
		System.out.println("Arguments");
		System.out.println("File[] inputFiles: " + Arrays.toString(inputFiles));
		System.out.println("String PDFfilenames: " + PDFfilenames);
		System.out.println("String folderDest: " + folderDest);
		System.out.println("-----------------");
		
        if (!new File(folderDest).exists())
        {
            new File(folderDest).mkdir();
        }
				
        String[] nomiPDF = PDFfilenames.split(" , ");
        for (int index = 0; index < nomiPDF.length; index++) {        	
            System.out.println("Conversion to text from file \"" + inputFiles[index] + "\"");
            long time = System.currentTimeMillis();
            
            String pdfFile = inputFiles[index].getName();
            
            String parsedText = this.getTextFromPDF(inputFiles[index], this.rotation_degree, folderDest);
            
            List<String> header = null;
            ArrayList<String[]> results = new ArrayList<String[]>();
            
            if(parsedText != null){
            	if (this.TXT_enabled){
                	String textFile = folderDest + "\\" + inputFiles[index].getName().substring(0, pdfFile.length() -3 ) + "txt";
            		this.writeTXT(new File(textFile), parsedText, this.TXT_encoding, this.TXT_append);
            		if (this.debug)
            			System.out.println("TXT created in " + ((System.currentTimeMillis() - time) / 1000.0) + " s");
            	}
                
                HashMap<String, String> result = this.pattern_match(pdfFile, parsedText);
                System.out.println("---------------------------------------------------------------------");
                
        		for(Map.Entry<String, String> entry : result.entrySet())
        		{
        		    String key = entry.getKey();
        		    String value = entry.getValue();
        		    
        		    System.out.println("k: " + key + "\t\tv: " + value);
        		}
        		
        		header = this.getResultHeader(result);
        		results.add(this.getResultData(result));
        		
                this.renamePDF(inputFiles[index], folderDest, result);
            }
            
            String csvFile = folderDest + "\\" + this.CSV_filename;
            this.writeCSV(csvFile, header, results);
                            
            System.out.println("PDF file processed in: " + ((System.currentTimeMillis() - time) / 1000.0) + " s");
        }
        return true;
    } //match

		
    private String getTextFromPDF(File pf, int rotation, String folderDest){
    	String parsedText = null;
	    try {
	    	PDDocument pdDoc = PDDocument.load(pf);
	    	if(rotation != 0){
		        PDPageTree pages = pdDoc.getDocumentCatalog().getPages();

		        for (PDPage page: pages)
		        {
		        	page.setRotation(rotation);
		        }	        
	    	}
	        parsedText = new PDFTextStripper().getText(pdDoc);
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
    
    private List<String> getResultHeader(HashMap<String, String> results){
    	return new ArrayList<String>(results.keySet());
	}

    private String[] getResultData(HashMap<String, String> results){
    	List<String> list_values = new ArrayList<String>();
    	for(String value : results.values()){
    		list_values.add(value);
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
			}//header
			
            printer.println();
            for (String[] line : values)
            	for (String val: line)
					printer.print(val);
            
			printer.flush();
			printer.close();
			System.out.println("Written to \""+ filename + "\"");
			
        } catch (IOException e) {
            e.printStackTrace();
        }   	
    } //writeCSV
    
    private void renamePDF(File pf, String folderDest, HashMap<String, String> result){
		File file_tmp = new File(new File(folderDest), pf.getName());
		String base_dest_filename = "";
		for (String i: this.PDFformat){
			base_dest_filename += result.get(i) + this.copyPDFsep;
		}
		base_dest_filename = base_dest_filename.trim();
		
		int i = 1;
		File file_dest;
		String dest_filename;
		do{
			if(i == 1){
				dest_filename = (base_dest_filename + ".pdf").replaceAll(this.copyPDFETL_from, this.copyPDFETL_to);
				file_dest = new File(new File(folderDest), dest_filename);
			}
			else
			{
				dest_filename = (base_dest_filename + this.copyPDFsep + i + ".pdf").replaceAll(this.copyPDFETL_from, this.copyPDFETL_to);
				file_dest = new File(new File(folderDest), dest_filename);
			}
			i++;
		}while(file_dest.exists());
		
		
		if(file_tmp.renameTo(file_dest))
			System.out.println("PDF moved to \""+ dest_filename + "\"");
		else
			System.out.println("PDF rename error to \""+ dest_filename + "\"");
    }

	
}//class
