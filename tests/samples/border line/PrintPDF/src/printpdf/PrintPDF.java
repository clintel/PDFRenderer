package printpdf;

import java.io.*;
import java.nio.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

import java.awt.geom.*;
import java.awt.print.*;

import com.sun.pdfview.*;

public class PrintPDF {

    public static void main(String[] args) throws IOException, PrinterException {

        String filePath =
            "C:\\Documents and Settings\\tomoke\\Desktop\\pdf test\\tests\\samples\\border line\\Test.pdf";
        String printerName = "Samsung ML-3050 Series PS";
        int copies = 1;
        PrintPDF printPDFFile = new PrintPDF();
        printPDFFile.printPDFFormat(filePath, printerName, copies);
    }

    public void printPDFFormat(String filePath, String printerName, int copies) {

        PrintService service = null;
        AttributeSet aset = null;
        FileInputStream inputStream = null;
        byte[] pdfContent = null;
        PDFFile pdfFile = null;
        PDFPrintPage pages = null;
        Book book = null;
        Paper paper = null;
        PrinterJob pjob = null;
        ByteBuffer bb = null;
        PrintService[] services = null;

        try {
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
            if (pdfFile.getNumPages() >= 1 && ( (pdfFile.getPage(0).getWidth()) > (pdfFile.getPage(0).getHeight()))) {
                pf.setOrientation(PageFormat.LANDSCAPE);
            }
            double iX = paper.getImageableX();
            double iY = paper.getImageableY();
            double iWidth = paper.getImageableWidth();
            double iHeight = paper.getImageableWidth();
            Rectangle2D area = pdfFile.getPage(0).getBBox();
            paper.setImageableArea(0, 0, pdfFile.getPage(0).getWidth(), pdfFile.getPage(0).getHeight());
            pf.setPaper(paper);
            pjob = PrinterJob.getPrinterJob();
            pjob.setPageable(book);
            pjob.setCopies(copies);
            services = PrintServiceLookup.lookupPrintServices(null, aset);
            if (services != null && services.length > 0) {
                service = services[0];
            }
            else {
                service = PrintServiceLookup.lookupDefaultPrintService();
            }
//            pf = PrinterJob.getPrinterJob().pageDialog(pf);
            pjob.setPrintService(service);
            pjob.setJobName(filePath);

            pf = pjob.validatePage(pf);
            book.append(pages, pf, pdfFile.getNumPages());
            pjob.setPageable(book);
            pjob.print();
            System.out.println("Done." + "Printer Name=" + service.getName());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            if (bb != null) {
                bb.clear();
                bb = null;
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            service = null;
            aset = null;
            pdfContent = null;
            pdfFile = null;
            pages = null;
            book = null;
            paper = null;
            pjob = null;
            services = null;
        }
    }
}
