import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.standard.PrinterName;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPrintPage;

public class PrintPDF {

	public static void main(String[] args) throws IOException, PrinterException {
		
		String filePath = "C:\\Documents and Settings\\prashantp\\Desktop\\Test.pdf";
		String printerName = "ITC_6thFLR";
		int copies=1;
		PrintPDF printPDFFile = new PrintPDF();
		printPDFFile.printPDFFormat(filePath,printerName,copies);
	}

	public void printPDFFormat(String filePath,String printerName,int copies) {
		
		PrintService service = null;
		AttributeSet aset=null;
		FileInputStream inputStream=null;
		byte[] pdfContent=null;
		PDFFile pdfFile=null;
		PDFPrintPage pages=null;
		Book book=null;
		Paper paper=null;
		PrinterJob pjob=null;
		ByteBuffer bb=null;
		PrintService[] services=null;
		
		try{	
			
			aset = new HashAttributeSet();
			book = new Book();
			paper = new Paper();
			aset.add(new PrinterName(printerName, null));
			inputStream = new FileInputStream(filePath);
			pdfContent = new byte[inputStream.available()];			
			inputStream.read(pdfContent, 0, inputStream.available());
			bb = ByteBuffer.wrap(pdfContent);
			pdfFile = new PDFFile(bb);
			pages = new PDFPrintPage(pdfFile);						
			PageFormat pf = PrinterJob.getPrinterJob().defaultPage();
			if (pdfFile.getNumPages()>=1 && ( (pdfFile.getPage(0).getWidth()) > (pdfFile.getPage(0).getHeight())) ){
				pf.setOrientation(PageFormat.LANDSCAPE);
			}	
			book.append(pages, pf, pdfFile.getNumPages());	
			paper.setImageableArea(0, 0, pdfFile.getPage(0).getWidth(), pdfFile.getPage(0).getHeight());
			pf.setPaper(paper);		
			pjob = PrinterJob.getPrinterJob();
			pjob.setPageable(book);
			pjob.setCopies(copies);			
			services = PrintServiceLookup.lookupPrintServices(null,aset);
			if (services != null && services.length > 0) {
				service = services[0];
			} else {
				service = PrintServiceLookup.lookupDefaultPrintService();
			}
			pjob.setPrintService(service);
			pjob.print();
			System.out.println("Done."+"Printer Name="+service.getName());
			
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			if(bb!=null){
				bb.clear();
				bb=null;
			}
			try {
				if(inputStream!=null){
					inputStream.close();
					inputStream=null;
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
			service = null;
			aset=null;
			pdfContent=null;
			pdfFile=null;
			pages=null;
			book=null;
			paper=null;
			pjob=null;
			services=null;
		}
	}
}
