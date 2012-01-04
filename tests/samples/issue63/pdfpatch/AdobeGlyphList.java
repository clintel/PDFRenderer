
package com.sun.pdfview.font.ttf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;


public class AdobeGlyphList {

	private static Map<String, Integer> name2index = new HashMap<String, Integer>();
	
	static {
		try {
			File f = new File("resources/glyphlist.txt");
			BufferedReader input =  new BufferedReader(new FileReader(f));
			try {
				String line;
				
				while (( line = input.readLine()) != null) {
					
					if(line.startsWith("#")) {
						continue;
					}
					
					StringTokenizer st = new StringTokenizer(line, " ;\r\n\t\f");
					
	                if (!st.hasMoreTokens()) {
	                    continue;
	                }
	                String name = st.nextToken();
	                
	                if (!st.hasMoreTokens()) {
	                    continue;
	                }
	                String index = st.nextToken();
					
					name2index.put(name, Integer.valueOf(index, 16));						
				}
			}
			finally {
				input.close();
			}
		}
		catch (Exception e){
			e.printStackTrace();			
		}
	}
	
	public static Integer getGlyphIndexByName(String name) {
		return name2index.get(name);
	}
}