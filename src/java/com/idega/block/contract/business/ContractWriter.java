package com.idega.block.contract.business;

import java.util.*;
import java.io.*;
import java.sql.SQLException;
import com.idega.io.MemoryOutputStream;
import com.idega.io.MemoryInputStream;
import com.idega.io.MemoryFileBuffer;

import com.idega.core.data.ICFile;
import com.idega.core.data.ICMimeType;
import com.idega.block.contract.data.*;
import com.idega.data.EntityFinder;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.IWBundle;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Phrase;
import com.lowagie.text.Chapter;
import com.lowagie.text.Element;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Chunk;
import com.lowagie.text.PageSize;
import com.lowagie.text.Section;
/**
 * Title:        idegaclasses
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      idega
 * @author <a href="aron@idega.is">Aron Birkir</a>
 * @version 1.0
 */

public class ContractWriter{

	public final static String contract_starts = "Valid from";
  public final static String contract_ends = "Valid to";
  public final static String today = "Today";

  public static int writePDF(int[] ids,int iCategoryId,String fileName,Font titleFont,Font paragraphFont, Font tagFont,Font textFont){
    boolean returner = false;
    boolean bEntity = false;
		int id = -1;
    if(ids != null &&  ids.length > 0){
      bEntity = true;
    }
    try {

				MemoryFileBuffer buffer = new MemoryFileBuffer();
        MemoryOutputStream mos = new MemoryOutputStream(buffer);
				MemoryInputStream mis = new MemoryInputStream(buffer);
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, mos);
        document.addAuthor("Idegaweb");
        document.addSubject("Contract");
        document.open();
        document.newPage();

        HeaderFooter  footer = new HeaderFooter(new Phrase("",textFont),true);
        footer.setBorder(0);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.setFooter(footer);

        ContractCategory ct = ContractFinder.getContractCategory(iCategoryId);
        List L = listOfTexts(iCategoryId);
        String title = "";
        if(ct != null)
          title = ct.getName()+" \n\n";
        Paragraph cTitle = new Paragraph(title , titleFont);
        // for each contract id
        for (int j = 0; j < ids.length; j++) {
          document.setPageCount(1);
          bEntity = ids[j] > 0 ;
          Chapter chapter = new Chapter(cTitle, 1);
          chapter.setNumberDepth(0);
          Paragraph P,P2;
          Chapter subChapter;
          Section subSection;
          Phrase phrase;
          //System.err.println("inside chapter : "+ids[j]);
          Hashtable H = getHashTags(iCategoryId,ids[j]);
          if(L!=null){
            int len = L.size();
            for (int i = 0; i < len; i++) {
              ContractText CT = (ContractText) L.get(i);
              P = new Paragraph(new Phrase(CT.getName(),paragraphFont));
              subSection = chapter.addSection(P,0);

              String sText = CT.getText();
              if(bEntity &&CT.getUseTags()){
                phrase = detagParagraph(H,sText,textFont,tagFont);
              }
              else{
                phrase = new Phrase(sText,textFont);
              }
              P2 = new Paragraph(phrase);
              subSection.add(P2);

            }
            if(bEntity){
              try {
                Contract eContract = new Contract(ids[j]);
                if(eContract.getStatus().equalsIgnoreCase(eContract.statusCreated)){
                  eContract.setStatusPrinted();
                  eContract.update();
                }
              }
              catch (SQLException ex) {
                ex.printStackTrace();
              }
            }
          }
          document.add(chapter);
          document.newPage();
        }
        document.close();

				if(ids.length == 1 && ids[0] > 0){
				  Contract C = new Contract(ids[0]);
					ICFile file = new ICFile();
					file.setFileValue(mis);
					file.setMimeType("application/x-pdf");
					file.setName(fileName+".pdf");
					file.setFileSize(buffer.length());
					file.insert();
					file.addTo(C);
					id = file.getID();
				}

        try {
          mos.close();
        }
        catch (Exception ex) {
          ex.printStackTrace();
        }
        returner = true;
    }
    catch (Exception ex) {
      ex.printStackTrace();
      returner = false;
    }
    return id;
  }

  public static int writeTestPDF(int iCategoryId,String fileName,  Font titleFont,Font paragraphFont, Font tagFont,Font textFont){
    return writePDF(new int[0],iCategoryId,fileName, titleFont, paragraphFont, tagFont, textFont);
  }
  public static int writePDF(int id,int iCategoryId,String fileName, Font titleFont,Font paragraphFont, Font tagFont,Font textFont){
      int[] ids = {id};
    return writePDF(ids,iCategoryId ,fileName, titleFont, paragraphFont, tagFont, textFont);
  }

  private static java.util.List listOfTexts(int iCategoryId){
    return ContractFinder.listOfContractTextsOrdered(iCategoryId);
  }

  private static Phrase detagParagraph(Map H,String sParagraph,Font textFont,Font tagFont){
    Phrase phrase = new Phrase();
    StringTokenizer ST  = new StringTokenizer(sParagraph,"[]");
    while(ST.hasMoreTokens()){
      String token = ST.nextToken();
      if(H.containsKey(token)){
				String value = (String)H.get(token);
        phrase.add(new Chunk(value,tagFont));
      }
      else{
        phrase.add(new Chunk(token,textFont));
      }
    }
    return phrase;
  }

  private static Hashtable getHashTags(int iCategoryId,int iContractId){
    try{
			List L = getTags(iCategoryId);
			Contract C = ContractFinder.getContract(iContractId);
			java.text.DateFormat dfLong = java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG);
			if(L!=null && C!=null){
        Hashtable H = new Hashtable(L.size());
				Iterator I = L.iterator();
				while(I.hasNext()){
					ContractTag tag = (ContractTag) I.next();
					String value = C.getMetaData(String.valueOf(tag.getID()));
					if(value !=null){
					  H.put(tag.getName(),value);
						//System.err.println("key "+tag.getName() +" value "+value);
					}
				}
				H.put(today,dfLong.format(new java.util.Date()));
				H.put(contract_starts,dfLong.format(C.getValidFrom()));
				H.put(contract_ends,dfLong.format(C.getValidTo()));
        return H;
			}
    }
    catch(Exception ex){
      ex.printStackTrace();
    }
		return new Hashtable();
  }

  private static List getTags(int iCategoryId){
    return ContractFinder.listOfContractTags(iCategoryId);
  }

}