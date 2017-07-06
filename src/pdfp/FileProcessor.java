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

public class FileProcessor{
	public static String DEFAULT_FILENAME = "Default.properties";
	public static String PREFIX_PATTERN = "pattern.";
	
	public boolean debug;
	public String propertiesFile;
	private String patterns_prefix;
	private String patterns_separator;
	private int rotation_degree;
	private boolean TXT_enabled;
	private boolean TXT_append;
	private String TXT_encoding;
	private String CSV_filename;
	private boolean CSV_append;
	private String filename_entry;
	private String ETL_from;
	private String ETL_to;
	private HashMap<String, String[]> patterns;

	public FileProcessor(){
		this(DEFAULT_FILENAME);
	}
	
	public FileProcessor(String propertiesFile){
		this.CSV_append = true;
		this.debug = true;
		this.propertiesFile = propertiesFile;
		this.patterns_separator = ", ";
		this.rotation_degree = 90;
		this.patterns_prefix = PREFIX_PATTERN;
		this.TXT_enabled = false;
		this.TXT_append = false;
		this.TXT_encoding = "UTF-8";
		this.CSV_filename = "output.csv";
		this.filename_entry = "filename";
		this.ETL_from = "\\r\\n|\\r|\\n";
		this.ETL_to = " ";
		
		if(!new File(this.propertiesFile).exists()){
			this.loadDefaultPatterns();
			this.writeProperties();
		}
		this.readProperties();
		
		System.out.println(this);
	}
		
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
		this.patterns.put("numero di fattura", (new String[]{
				"Bolletta n. \n(.)\\.*",
				"Segue bolletta Hera Comm n. (\\d{1,12})",
				"Fattura n. (\\d{1,12}) del"
				}));
		this.patterns.put("data fattura", (new String[]{
				"Segue bolletta Hera Comm n. .* del (\\d{1,2}.\\d{1,2}.\\d{1,4})",
				"Data emissione (\\d{1,2}.\\d{1,2}.\\d{1,4})",
				"Data emissione (\\d{1,2}.\\d{1,2}.\\d{1,4})\\.*"
				}));
		this.patterns.put("comune", (new String[]{
				"(?m)Servizio fornito in.*\\s{1,2}\\d{5} (\\p{Print}*) \\.*",
				"(?m)Servizio fornito in.*\\s{1,2}\\d{5} (\\p{Print}*)",
				"(?m)Servizio fornito in.*\\s{1,2}\\w*\\s{1,2}\\d{5} (\\p{Print}*) \\.*",
				"(?m)Servizio fornito in.*\\s{1,2}.*\\s{1,2}\\d{5} (\\p{Print}*) \\.*",
				"\\d{5} ([A-Z]*) \\w{2}\\.*",
				"\\d{5} ([A-Z]*)\\.*",
				"\\d{5}  ([A-Z]*)  \\w{2}\\.*"
				}));
		this.patterns.put("pod", (new String[]{
				"POD \\(Punto di prelievo\\): (\\w{1,14})\\.+",
				"POD \\(Punto di prelievo\\): (\\w{1,14})\\.*",
				".*POD (\\w{1,14})", 
				"IT001E(\\w{4,8})"
				}));
		this.patterns.put("totale fattura", (new String[]{
				"Totale bolletta Hera Comm (.*) €", 
				"Totale bolletta/contratto (.*) €",
				"\\s{1,2}Totale da pagare (.*) €"
				}));
		this.patterns.put("imponibile", (new String[]{
				"Aliquota IVA 22% (.*) ",
				"Totale netto IVA (.*) €",
				"22% IVA vendite 22% mensile ((\\d{1,3}.)?\\d{1,3},\\d{1,2})",
				"IVA 22% su base imponibile .* € (.*) €"
				}));
		this.patterns.put("consumi in kWh", (new String[]{
				"Consumo fatturato \\(Chilowatt orari\\).*,\\d{1,3} ((\\d{1,3}.)?\\d{1,3},\\d{1,3}) kWh",
				"(?m)totale\\s{1,2}kWh (\\p{Print}*)",
				",\\d{1,3} ((\\d{1,3}.)?\\d{1,3},\\d{1,3}) kWh",
				"consumo rilevato dal \\.*,\\d{1,3} ((\\d{1,3}.)?\\d{1,3},\\d{1,3}) kWh",
				"Consumo rilevato dal \\.*,\\d{1,3} ((\\d{1,3}.)?\\d{1,3},\\d{1,3}) kWh"
				}));
		this.patterns.put("periodo di competenza", (new String[]{
				"consumo rilevato (dal \\d{1,2}.\\d{1,2}.\\d{1,4} al \\d{1,2}.\\d{1,2}.\\d{1,4} \\(\\d{1,3} giorni\\))",
				"Consumo rilevato (dal \\d{1,2}.\\d{1,2}.\\d{1,4} al \\d{1,2}.\\d{1,2}.\\d{1,4} \\(\\d{1,3} giorni\\))",
				"consumo stimato (dal \\d{1,2}.\\d{1,2}.\\d{1,4} al \\d{1,2}.\\d{1,2}.\\d{1,4} \\(\\d{1,3} giorni\\))",
				"Consumo stimato (dal \\d{1,2}.\\d{1,2}.\\d{1,4} al \\d{1,2}.\\d{1,2}.\\d{1,4} \\(\\d{1,3} giorni\\))",
				"(?m)Stiamo ricalcolando consumi .* (dal\\s{1,3}\\d{1,2}.\\d{1,2}.\\d{1,4} al \\d{1,2}.\\d{1,2}.\\d{1,4})"
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
            
            String parsedText = this.getTextFromPDF(inputFiles[index], this.rotation_degree);
            
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
            }
            
            String csvFile = folderDest + "\\" + this.CSV_filename;
            this.writeCSV(csvFile, header, results);
                            
            System.out.println("PDF file processed in: " + ((System.currentTimeMillis() - time) / 1000.0) + " s");
        }
        return true;
    } //match

		
    private String getTextFromPDF(File pf, int rotation){
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
	    	BufferedWriter writer = new BufferedWriter(new FileWriter(filename, this.CSV_append));
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
    }
	
}//class
