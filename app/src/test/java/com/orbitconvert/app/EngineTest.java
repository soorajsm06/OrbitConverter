package com.orbitconvert.app;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;
import com.tom_roush.pdfbox.pdmodel.*;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class EngineTest {
 File work,out;
 @Before public void setup()throws Exception{work=Files.createTempDirectory("orbit-test").toFile();out=new File(work,"out");out.mkdir();PDFBoxResourceLoader.init(RuntimeEnvironment.getApplication());}
 @After public void clean(){MainActivity.delete(work);}
 File input(String name,String text)throws Exception{File f=new File(work,name);Files.write(f.toPath(),text.getBytes(StandardCharsets.UTF_8));return f;}
 List<File> run(String action,File... files)throws Exception{return new ConvertEngine(RuntimeEnvironment.getApplication(),out,s->{}).run(action,Arrays.asList(files),new ConvertEngine.Options());}
 @Test public void quotedCsvRoundTrip()throws Exception{String s="name,note\r\n\"A,B\",\"line1\nline2\"\r\n";List<List<String>> rows=ConvertEngine.parseTable(s,',');assertEquals("line1\nline2",rows.get(1).get(1));assertEquals(rows,ConvertEngine.parseTable(ConvertEngine.tableText(rows,','),','));}
 @Test public void csvJsonPreservesValues()throws Exception{File f=run("Table to JSON",input("a.csv","name,value\nAlice,001\n")).get(0);assertTrue(ConvertEngine.readText(f).contains("001"));}
 @Test(expected=IOException.class) public void duplicateHeadersRejected()throws Exception{run("Table to JSON",input("a.csv","a,a\n1,2"));}
 @Test public void zipRoundTrip()throws Exception{File z=run("Create ZIP",input("a.txt","hello"),input("b.txt","world")).get(0);File extracted=new File(work,"extracted");extracted.mkdir();List<File> r=new ConvertEngine(RuntimeEnvironment.getApplication(),extracted,s->{}).run("Extract ZIP",Collections.singletonList(z),new ConvertEngine.Options());assertEquals(2,r.size());assertEquals("hello",ConvertEngine.readText(new File(extracted,"a.txt")));}
 @Test(expected=IOException.class) public void zipTraversalRejected()throws Exception{File f=new File(work,"evil.zip");try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(f))){z.putNextEntry(new ZipEntry("../escape.txt"));z.write(1);z.closeEntry();}run("Extract ZIP",f);}
 @Test public void docxRoundTripText()throws Exception{File d=run("Text to DOCX",input("note.txt","Hello & <world>\nSecond line")).get(0);File next=new File(work,"next");next.mkdir();List<File> r=new ConvertEngine(RuntimeEnvironment.getApplication(),next,s->{}).run("DOCX to TXT",Collections.singletonList(d),new ConvertEngine.Options());assertEquals("Hello & <world>\nSecond line\n",ConvertEngine.readText(r.get(0)));}
 @Test public void splitAndMergePdf()throws Exception{File pdf=new File(work,"sample.pdf");try(PDDocument d=new PDDocument()){d.addPage(new PDPage(PDRectangle.A4));d.addPage(new PDPage(PDRectangle.LETTER));for(int i=0;i<2;i++){try(PDPageContentStream content=new PDPageContentStream(d,d.getPage(i))){content.beginText();content.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA,12);content.newLineAtOffset(50,700);content.showText("Page "+(i+1)+" original text");content.endText();}}d.save(pdf);}List<File> split=run("Split PDF",pdf);assertEquals(2,split.size());File merged=new File(work,"merge");merged.mkdir();List<File> result=new ConvertEngine(RuntimeEnvironment.getApplication(),merged,s->{}).run("Merge PDF",split,new ConvertEngine.Options());try(PDDocument d=PDDocument.load(result.get(0))){assertEquals(2,d.getNumberOfPages());String text=new com.tom_roush.pdfbox.text.PDFTextStripper().getText(d);assertTrue(text.contains("Page 1 original text"));assertTrue(text.contains("Page 2 original text"));}}
 @Test public void subtitleTimeConversion()throws Exception{File result=run("VTT to SRT",input("a.vtt","WEBVTT\n\n00:01.000 --> 00:02.500\nHello\n")).get(0);assertTrue(ConvertEngine.readText(result).contains("00:00:01,000 --> 00:00:02,500"));}
 @Test(expected=IOException.class) public void copyLimitEnforced()throws Exception{ConvertEngine.copy(new ByteArrayInputStream(new byte[10]),new ByteArrayOutputStream(),5);}
}
