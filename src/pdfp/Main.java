package pdfp;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Label;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.filechooser.FileFilter;
import pdfp.FileProcessor;

public class Main extends JFrame {
	// private static final String DEFAULT_TIMEZIONE = "UTC";
	private static final String DEFAULT_TIMEZIONE = "Europe/Rome";
	public static final String PropertieFilesExtension = ".properties";
	
    private static final long serialVersionUID = 1;
    private JButton btnProcess;
    private JButton btnExit;
    private JButton btnBrowse1;
    private JButton btnBrowse2;
    private JComboBox<String> cmbFornitore;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JPanel jPanel3;
    private Label lblPathExport;
    private Label lblPathPDF;
    private JTextField txtPathExport;
    public JTextField txtPathPDF;
    private File[] inputFiles;
    private String folderDest;
    private String PDFnames;
    private String ClassPath;
    
    private String[] filelist = null;

    public Main() {
        this.initComponents();
    }
    
	public static String printDate()
	{
		TimeZone tz = TimeZone.getTimeZone(DEFAULT_TIMEZIONE);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		return df.format(new Date()); 
	}

    private void initComponents() {
        this.jPanel1 = new JPanel();
        new JScrollPane();
        new JScrollPane();
        this.jPanel2 = new JPanel();
        this.lblPathPDF = new Label();
        this.txtPathPDF = new JTextField();
        this.txtPathExport = new JTextField();
        this.lblPathExport = new Label();
        this.btnBrowse1 = new JButton();
        this.btnBrowse2 = new JButton();
        this.jPanel3 = new JPanel();
        this.cmbFornitore = new JComboBox<String>();
        this.btnExit = new JButton();
        this.btnProcess = new JButton();
        this.setDefaultCloseOperation(3);
        this.setTitle("PDF processor");
        this.setResizable(false);
        this.jPanel2.setBorder(BorderFactory.createTitledBorder("Insert INPUT and OUTPUT file"));
        this.lblPathPDF.setText("Input PDF file(s)");
        this.txtPathPDF.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                Main.this.txtPathPDFActionPerformed(evt);
            }
        });
        this.lblPathExport.setText("Output file path");
        this.btnBrowse1.setText("Browse");
        this.btnBrowse1.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                Main.this.btnBrowse1ActionPerformed(evt);
            }
        });
        this.btnBrowse2.setText("Browse");
        this.btnBrowse2.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                Main.this.btnBrowse2ActionPerformed(evt);
            }
        });
        GroupLayout jPanel2Layout = new GroupLayout(this.jPanel2);
        this.jPanel2.setLayout(jPanel2Layout);
        JScrollBar scrollBarPdf = new JScrollBar(0);
        BoundedRangeModel brm = this.txtPathPDF.getHorizontalVisibility();
        scrollBarPdf.setModel(brm);
        JScrollBar scrollBarExport = new JScrollBar(0);
        BoundedRangeModel brmExport = this.txtPathExport.getHorizontalVisibility();
        scrollBarExport.setModel(brmExport);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addContainerGap().addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(this.lblPathExport, -2, -1, -2).addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false).addComponent(this.txtPathPDF, GroupLayout.Alignment.LEADING, -1, 328, 32767).addComponent(this.txtPathExport, GroupLayout.Alignment.LEADING, -1, 328, 32767))).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(this.btnBrowse1).addComponent(this.btnBrowse2))).addComponent(this.lblPathPDF, -2, -1, -2)).addGap(15, 15, 15)));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addComponent(this.lblPathPDF, -2, -1, -2).addGap(2, 2, 2).addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(this.btnBrowse2, GroupLayout.Alignment.TRAILING).addGroup(jPanel2Layout.createSequentialGroup().addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(this.txtPathPDF, -2, -1, -2).addComponent(this.btnBrowse1)).addGap(18, 18, 18).addComponent(this.lblPathExport, -2, -1, -2).addGap(2, 2, 2).addComponent(this.txtPathExport, -2, -1, -2))).addContainerGap(-1, 32767)));
        this.lblPathPDF.getAccessibleContext().setAccessibleName("PDF path");
        this.jPanel3.setBorder(BorderFactory.createTitledBorder("Processors"));
		this.cmbFornitore.setModel(new DefaultComboBoxModel<String>(this.listProcessorFiles()));
        this.cmbFornitore.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                Main.this.cmbFornitoreActionPerformed(evt);
            }
        });
        GroupLayout jPanel3Layout = new GroupLayout(this.jPanel3);
        this.jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addContainerGap().addComponent(this.cmbFornitore, -2, 119, -2).addContainerGap(28, 32767)));
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addContainerGap().addComponent(this.cmbFornitore, -2, -1, -2).addContainerGap(-1, 32767)));
        this.btnExit.setText("Exit");
        this.btnExit.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                Main.this.btnExitActionPerformed(evt);
            }
        });
        this.btnProcess.setText("Process");
        this.btnProcess.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                Main.this.btnProcessActionPerformed(evt);
            }
        });
        GroupLayout layout = new GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(this.jPanel3, -2, -1, -2).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 132, 32767).addComponent(this.btnProcess).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(this.btnExit, -2, 62, -2)).addComponent(this.jPanel2, GroupLayout.Alignment.TRAILING, 0, 442, 32767).addComponent(this.jPanel1, GroupLayout.Alignment.TRAILING, -1, -1, 32767)).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addComponent(this.jPanel1, -2, -1, -2).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(this.jPanel2, -2, -1, -2).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(this.btnProcess).addComponent(this.btnExit)).addComponent(this.jPanel3, -2, -1, -2)).addGap(187, 187, 187)));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setBounds((screenSize.width - 470) / 2, (screenSize.height - 544) / 2, 470, 544);
    }

    private void txtPathPDFActionPerformed(ActionEvent evt) {
    }

    private void btnExitActionPerformed(ActionEvent evt) {
        System.exit(0);
    }

    private void btnBrowse1ActionPerformed(ActionEvent evt) {
        this.ApplicationPDF();
    }

    private void btnBrowse2ActionPerformed(ActionEvent evt) {
        this.ApplicationExport();
    }

    private void btnProcessActionPerformed(ActionEvent evt) {
    	System.out.println("Main START: " + Main.printDate());
        this.FindPath();
        int index = this.cmbFornitore.getSelectedIndex();
        if (this.PDFnames == null || this.PDFnames.equals("")) {
            JOptionPane.showMessageDialog(null, "Invavlid PDF filename!");
        } else if (this.folderDest == null || this.folderDest.equals("")) {
            JOptionPane.showMessageDialog(null, "Invavlid PDF path");
        } else {
        	System.out.println("selected item " + index + " filename: " + this.filelist[index]);
        	FileProcessor g = new FileProcessor(this.filelist[index] + PropertieFilesExtension);
        	boolean result = g.match(this.inputFiles, this.folderDest);
			if(result == true)
				JOptionPane.showMessageDialog(null, "Success!");
			else
				JOptionPane.showMessageDialog(null, "Fail!");        	
        }
        System.out.println("Main END: " + Main.printDate());
    }

    private void cmbFornitoreActionPerformed(ActionEvent evt) {
    }

    public void ApplicationPDF() {
        JFileChooser fc = new JFileChooser();
        fc.setApproveButtonText("Open");
        fc.setDialogTitle("Select PDF input file(s)");
        fc.setFileSelectionMode(0);
        fc.setMultiSelectionEnabled(true);
        fc.setFileFilter(new PDFFileFilter());
        int value = fc.showOpenDialog(null);
        this.inputFiles = fc.getSelectedFiles();
        switch(value){
        	case 0:
        		this.txtPathPDF.setText(this.printNames());
        		break;
        	case 1:
        		System.out.println("Cancel");
        		break;
        	case -1:
        		System.out.println("ERROR");
        		break;
        }
    }

    public void ApplicationExport()
    {
        JFileChooser fc = new JFileChooser();
        fc.setApproveButtonText("Open");
        fc.setDialogTitle("Select the output path");
        fc.setFileSelectionMode(1);
        switch(fc.showOpenDialog(null))
        {
        	case 0:
	            File f = fc.getSelectedFile();
	            System.out.println("Select file: " + f);
	            this.txtPathExport.setText(f.toString());
	            this.folderDest = f.toString();
	            break;
        	case 1:
        		System.out.println("Cancel");
        		break;
        	case -1:
        		System.out.println("ERROR");
        		break;
        }
    }

    public String printNames()
    {
        String res = "";
        String separe = "";
        int i = 0;
        while (i < this.inputFiles.length) {
            String path = this.inputFiles[i].toString();
            String[] tmp = path.split("\\\\");
            if (i != 0) {
                separe = " , ";
            }
            res = String.valueOf(res) + separe + tmp[tmp.length - 1];
            ++i;
        }
        this.PDFnames = res;
        return res;
    }
    
    public String[] listProcessorFiles()
    {
    	if(this.filelist == null)
    	{
	        File folder = new File(".");
	        File[] listOfFiles = folder.listFiles();
	        
	        ArrayList<String> filelist_ar = new ArrayList<String>();
	        for (int i = 0; i < listOfFiles.length; i++) {
	            if (listOfFiles[i].isFile())
	            {
	            	String filename = listOfFiles[i].getName();
	            	if(filename.endsWith(PropertieFilesExtension))
	            	{
	            		filelist_ar.add(filename.replace(PropertieFilesExtension, ""));
	            		System.out.println("Processor file " + filename);
	            	}
	            }
	        }
	    	this.filelist = new String[filelist_ar.size()];
	    	this.filelist = filelist_ar.toArray(this.filelist);
    	}
        return this.filelist; 
    }

    private void FindPath()
    {
        String[] cp1 = System.getProperty("java.class.path").split(";");
        String[] cp2 = cp1[0].split("\\\\");
        String sep = "";
        int i = 0;
        while (i < cp2.length - 1) {
            if (i == 0) {
                sep = "";
            }
            this.ClassPath = String.valueOf(this.ClassPath) + sep + cp2[i];
            if (i == 0) {
                sep = "\\";
            }
            ++i;
        }
    }

    public static void main(String[] args)
    {
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run() {
                new Main().setVisible(true);
            }
        });
    }

    class PDFFileFilter extends FileFilter
    {
        PDFFileFilter() {
        }

        @Override
        public boolean accept(File pathname)
        {
            if (pathname.isDirectory())
            {
                return true;
            }
            if (pathname.getName().endsWith(".pdf"))
            {
                return true;
            }
            return false;
        }

        @Override
        public String getDescription()
        {
            return "PDF Document";
        }
    }

}