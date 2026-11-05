package com.orbitconvert.app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class CoreTest {
 @Test public void unicodeAndOpenPages(){assertEquals(Arrays.asList(0,1,2,4),PageRange.parse("१–३، ५-",5));}
 @Test public void reverseAndDuplicates(){assertEquals(Arrays.asList(0,1,2),PageRange.parse("3-1, 2",4));}
 @Test(expected=IllegalArgumentException.class) public void rejectOutOfRange(){PageRange.parse("10",3);}
 @Test(expected=IllegalArgumentException.class) public void rejectInvalid(){PageRange.parse("wrong",3);}
 @Test(expected=IllegalArgumentException.class) public void rejectEmptyToken(){PageRange.parse("1,,2",3);}
 @Test public void allPages(){assertEquals(Arrays.asList(0,1,2),PageRange.parse("",3));}
 @Test public void mixedTypesOnlyZip(){assertEquals(Collections.singletonList("Create ZIP"),Formats.actions(Arrays.asList("a.png","b.pdf")));}
 @Test public void pdfRoutes(){assertTrue(Formats.actions(Arrays.asList("a.pdf","b.PDF")).contains("PDF to long image"));}
 @Test public void unsupportedFormatNotAdvertised(){assertEquals(Collections.singletonList("Create ZIP"),Formats.actions(Collections.singletonList("design.dwg")));}
}
