/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Gunter Zeilinger, Huetteldorferstr. 24/10, 1150 Vienna/Austria/Europe.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunterze@gmail.com>
 * Michael Bassey <mabaze178@yaoo.com>
 * Edgar Luberenga <edgar@pacs.pro>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */





package org.dcm4che2.tool.xml2dcm;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.TransferSyntax;
import org.dcm4che2.io.ContentHandlerAdapter;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.util.CloseUtils;
import org.xml.sax.SAXException;


import org.dcm4che2.net.log_writer;


/**
 * @author gunter zeilinger(gunterze@gmail.com)
 * @version $Revision: 5551 $ $Date: 2007-11-27 15:24:27 +0100 (Tue, 27 Nov 2007) $
 * @since Aug 20, 2005
 *
 * On May 17, 2012, Michael Bassey & Edgar Luberenga made modifications to some sections of this code.
 * Sections modified include:
 * (1) Line 69 (Addition of new import statements)
 * (2) Lines 119 to 123 (Declaration/addition of new variables).
 * (3) Lines 127 to 152 (Definition/addition of new methods)
 * (4) Lines 210 to 251 (Mixes new statements with original ones. New statements are marked with bounded lines).
 * (5) Lines 255 to 261 (Disables a method from the original code).
 * (6) Lines 264 to 282 (Creation/addition of a new method that uses the name of a disbaled original method).
 * (5) Lines 285 to 373 (Disables a method from the original code).
 * (6) Lines 380 to 504 (Creation of a new method that borrows largely from an existent method).
 */






public class Xml2Dcm {

    private static final String USAGE = "xml2dcm [-geEuUVh] [-a|-d] [-t <tsuid>] " +
            "[-i <dcmfile>] [-x [<xmlfile>] -d <basedir>] -o <dcmfile>";
    private static final String DESCRIPTION = "Modify existing or create " +
            "new DICOM file according given XML presentation and store result " +
            "as ACR/NEMA-2 dump (option: -a) or DICOM Part 10 file " +
            "(option: -d). If neither option -a nor -d is specified, " +
            "inclusion of Part 10 File Meta Information depends, if the input " +
            "DICOM file or the XML presentation already includes File Meta " +
            "Information attributes (0002,eeee). Either option -i <dcmfile> or" +
            "-x [<xmlfile>] (or both) must be specified.\n" +
            "Options:";
    private static final String EXAMPLE = "\nExample: xml2dcm -x in.xml -o out.dcm\n" +
            " => Convert XML presentation in.xml to DICOM file out.dcm\n" +
            "xml2dcm -d -t 1.2.840.10008.1.2.1.99 -i in.dcm -o out.dcm\n" +
            " => Load DICOM object from file in.dcm and store it as " +
            "DICOM (Part 10) file encoded with Deflated Explicit VR Little " +
            "Endian Transfer Syntax.";


    //------------------------------------------
	private String logRoot = null;
	private String logName = null;
	private String ObjectState = "not_yet_set";
    //------------------------------------------



    //-------------------------------------------
    public String getLogRoot(){

	return this.logRoot;
	}

    public String getLogName(){

    return this.logName;
    }

    public Xml2Dcm get_thisObject(){

	return this;
	}

    public String get_ObjectState(){

	return this.ObjectState;
	}

	public void set_ObjectState(String value){

	this.ObjectState = value;
	}
	//-----------------------------------------


    private CommandLine parse(String[] args) {
        Options opts = new Options();
        Option ifile = new Option("i", true,
                "Update attributes in specified DICOM file instead " +
                "generating new one.");
        ifile.setArgName("dcmfile");
        opts.addOption(ifile);
        Option xmlfile = new Option("x", true,
                "XML input, used to update or generate new DICOM file." +
                "Without <xmlfile>, read from standard input.");
        xmlfile.setOptionalArg(true);
        xmlfile.setArgName("xmlfile");
        opts.addOption(xmlfile);
        Option basedir = new Option("d", true,
                "Directory to resolve external attribute values referenced by " +
                "XML read from standard input.");
        basedir.setArgName("basedir");
        opts.addOption(basedir);
        Option ofile = new Option("o", true,
                "Generated DICOM file or ACR/NEMA-2 dump");
        ofile.setArgName("dcmfile");
        opts.addOption(ofile);
        Option tsuid = new Option("t", true,
                "Store result with specified Transfer Syntax.");
        tsuid.setArgName("tsuid");
        opts.addOption(tsuid);
        opts.addOption("a", "acrnema2", false,
                "Store result as ACR/NEMA 2 dump. Mutual exclusive " +
                "with option -d");
        opts.addOption("d", "dicom", false,
                "Store result as DICOM Part 10 File. Mutual exclusive " +
                "with option -a");
        opts.addOption("g", "grlen", false,
                "Include (gggg,0000) Group Length attributes." +
                "By default, optional Group Length attributes are excluded.");
        opts.addOption("E", "explseqlen", false,
                "Encode sequences with explicit length. At default, non-empty " +
                "sequences are encoded with undefined length.");
        opts.addOption("e", "explitemlen", false,
                "Encode sequence items with explicit length. At default, " +
                "non-empty sequence items are encoded with\n" +
                "undefined length.");
        opts.addOption("U", "undefseqlen", false,
                "Encode all sequences with undefined length. Mutual exclusive " +
                "with option -E.");
        opts.addOption("u", "undefitemlen", false,
                "Encode all sequence items with undefined length. Mutual " +
                "exclusive with option -e.");
        opts.addOption("h", "help", false, "print this message");
        opts.addOption("V", "version", false,
                "print the version information and exit");
        CommandLine cl = null;
        try {
            cl = new PosixParser().parse(opts, args);
        } catch (ParseException e) {
            //exit("dcm2xml: " + e.getMessage());

            //-------------------------------------------------------
			exit("dcm2xml: " + e.getMessage(), this.get_thisObject());
            //-------------------------------------------------------
            throw new RuntimeException("unreachable");
        }
        if (cl.hasOption('V')) {
            Package p = Xml2Dcm.class.getPackage();
            System.out.println("dcm2xml v" + p.getImplementationVersion());
            //System.exit(0);

            //-------------------------------------------------------
			exit("dcm2xml v" + p.getImplementationVersion(), this.get_thisObject());
            //-------------------------------------------------------
        }
        if (cl.hasOption('h') || !cl.hasOption("o")
                || (!cl.hasOption("x") && !cl.hasOption("i"))) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(USAGE, DESCRIPTION, opts, EXAMPLE);
            //System.exit(0);

            //-------------------------------------------------------
		    exit("dcm2xml <error>", this.get_thisObject());
            //-------------------------------------------------------
        }
        if (cl.hasOption("a") && cl.hasOption("d"))
            //exit("xml2dcm: Option -a and -d are mutual exclusive");

            //-------------------------------------------------------
		    exit("xml2dcm: Option -a and -d are mutual exclusive", this.get_thisObject());
            //-------------------------------------------------------
        if (cl.hasOption("e") && !cl.hasOption("u"))
            //exit("xml2dcm: Option -e and -u are mutual exclusive");
            //-------------------------------------------------------
			exit("xml2dcm: Option -e and -u are mutual exclusive", this.get_thisObject());
            //-------------------------------------------------------
        if (cl.hasOption("E") && !cl.hasOption("U"))
            //exit("xml2dcm: Option -E and -U are mutual exclusive");
            //-------------------------------------------------------
			exit("xml2dcm: Option -E and -U are mutual exclusive", this.get_thisObject());
            //-------------------------------------------------------
        return cl;
    }

    /*
    private static void exit(String msg) {
        System.err.println(msg);
        System.err.println("Try 'xml2dcm -h' for more information.");
        System.exit(1);
    }
    */


   //-----------------------------------------------------------------------------------------------------
    private static void exit(String msg, Xml2Dcm xml2dcm_obj) {

    if(xml2dcm_obj != null)
      {
       //----------------------------------
	   log_writer.doLogging_QRmgr(xml2dcm_obj.getLogRoot(), xml2dcm_obj.getLogName(), "<exit> msg: "+msg);
       //-----------------------------------

       xml2dcm_obj.set_ObjectState("failed");
      }
    else
      {
       System.err.println(msg);
       System.err.println("Try 'xml2dcm -h' for more information.");
       System.exit(1);
	  }
    }
    //------------------------------------------------------------------------------------------------------


    /*
    public static void main(String[] args) {

		//----------------------------------------------
		for(int a = 0; a < args.length; a++)
	       {
		    System.out.println("args["+a+"]: "+args[a]);
		   }
		//----------------------------------------------

        CommandLine cl = parse(args);
        DicomObject dcmobj = new BasicDicomObject();
        if (cl.hasOption("i")) {
            File ifile = new File(cl.getOptionValue("i"));
            try {
                loadDicomObject(ifile, dcmobj);
            } catch (IOException e) {
                System.err.println("xml2dcm: failed to load DICOM file: "
                        + ifile+ ": " + e.getMessage());
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
        if (cl.hasOption("x")) {
            String xmlFile = cl.getOptionValue("x");
            try {
                parseXML(xmlFile, dcmobj, cl.getOptionValue("d"));
            } catch (FactoryConfigurationError e) {
                System.err.println("xml2dcm: Configuration Error: "
                        + e.getMessage());
                System.exit(1);
            } catch (ParserConfigurationException e) {
                System.err.println("xml2dcm: Configuration Error: "
                        + e.getMessage());
                System.exit(1);
            } catch (SAXException e) {
                System.err.println("xml2dcm: failed to parse XML from " +
                        (xmlFile != null ? xmlFile : " standard input")
                        + ": " + e.getMessage());
                e.printStackTrace(System.err);
                System.exit(1);
            } catch (IOException e) {
                System.err.println("xml2dcm: failed to parse XML from " +
                        (xmlFile != null ? xmlFile : " standard input")
                        + ": " + e.getMessage());
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
        File ofile = new File(cl.getOptionValue("o"));
        DicomOutputStream dos = null;
        try {
            dos = new DicomOutputStream(new BufferedOutputStream(
                    new FileOutputStream(ofile)));
            if (!dcmobj.command().isEmpty()) {
                dos.writeCommand(dcmobj);
                System.out.println("Created DICOM Command Set " + ofile);
            } else {
                dos.setIncludeGroupLength(cl.hasOption("g"));
                dos.setExplicitItemLength(cl.hasOption("e"));
                dos.setExplicitItemLengthIfZero(!cl.hasOption("u"));
                dos.setExplicitSequenceLength(cl.hasOption("E"));
                dos.setExplicitSequenceLengthIfZero(!cl.hasOption("U"));
                String tsuid = cl.getOptionValue("t");
                if (cl.hasOption("d")) {
                    if (tsuid == null)
                        tsuid = TransferSyntax.ExplicitVRLittleEndian.uid();
                    dcmobj.initFileMetaInformation(tsuid);
                }
                if (cl.hasOption("a") || dcmobj.fileMetaInfo().isEmpty()) {
                    if (tsuid == null)
                        tsuid = TransferSyntax.ImplicitVRLittleEndian.uid();
                    dos.writeDataset(dcmobj, TransferSyntax.valueOf(tsuid));
                    System.out.println("Created ACR/NEMA Dump " + ofile);
                } else {
                    dos.writeDicomFile(dcmobj);
                    System.out.println("Created DICOM File " + ofile);
                }
            }
        } catch (IOException e) {
            System.err.println("xml2dcm: failed to create " + ofile + ": "
                    + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        } finally {
            CloseUtils.safeClose(dos);
        }
     }
     */






    public void startOp(String[] args) {


        CommandLine cl = parse(args);
        DicomObject dcmobj = new BasicDicomObject();

        if(!(this.get_ObjectState().equals("failed"))){
        if (cl.hasOption("i")) {
            File ifile = new File(cl.getOptionValue("i"));
            try {
                loadDicomObject(ifile, dcmobj);
            } catch (IOException e) {
                System.err.println("xml2dcm: failed to load DICOM file: "
                        + ifile+ ": " + e.getMessage());
                e.printStackTrace(System.err);
                //System.exit(1);
                //-------------------------------------------------------
				exit("xml2dcm: failed to load DICOM file:" + ifile+ ": " + e.getMessage(), this.get_thisObject());
                //-------------------------------------------------------
            }
        }
        }



        if(!(this.get_ObjectState().equals("failed"))){
        if (cl.hasOption("x")) {
            String xmlFile = cl.getOptionValue("x");
            try {
                parseXML(xmlFile, dcmobj, cl.getOptionValue("d"));
            } catch (FactoryConfigurationError e) {
                System.err.println("xml2dcm: Configuration Error: "
                        + e.getMessage());
                //System.exit(1);

                //-------------------------------------------------------
				exit("xml2dcm: Configuration Error: "+ e.getMessage(), this.get_thisObject());
                //-------------------------------------------------------
            } catch (ParserConfigurationException e) {
                System.err.println("xml2dcm: Configuration Error: "
                        + e.getMessage());
                //System.exit(1);
                 //-------------------------------------------------------
				exit("xml2dcm: Configuration Error: "+ e.getMessage(), this.get_thisObject());
                //-------------------------------------------------------
            } catch (SAXException e) {
                System.err.println("xml2dcm: failed to parse XML from " +
                        (xmlFile != null ? xmlFile : " standard input")
                        + ": " + e.getMessage());
                e.printStackTrace(System.err);
                //System.exit(1);
                 //-------------------------------------------------------
				exit("xml2dcm: failed to parse XML from "+ (xmlFile != null ? xmlFile : " standard input")
                        + ": " + e.getMessage(), this.get_thisObject());
                //-------------------------------------------------------
            } catch (IOException e) {
                System.err.println("xml2dcm: failed to parse XML from " +
                        (xmlFile != null ? xmlFile : " standard input")
                        + ": " + e.getMessage());
                e.printStackTrace(System.err);
                //System.exit(1);

              //-------------------------------------------------------
				exit("xml2dcm: failed to parse XML from "+ (xmlFile != null ? xmlFile : " standard input")
                        + ": " + e.getMessage(), this.get_thisObject());
              //-------------------------------------------------------

            }
        }
        }


        if(!(this.get_ObjectState().equals("failed"))){
        File ofile = new File(cl.getOptionValue("o"));
        DicomOutputStream dos = null;
        try {
            dos = new DicomOutputStream(new BufferedOutputStream(
                    new FileOutputStream(ofile)));
            if (!dcmobj.command().isEmpty()) {
                dos.writeCommand(dcmobj);
                System.out.println("Created DICOM Command Set " + ofile);
            } else {
                dos.setIncludeGroupLength(cl.hasOption("g"));
                dos.setExplicitItemLength(cl.hasOption("e"));
                dos.setExplicitItemLengthIfZero(!cl.hasOption("u"));
                dos.setExplicitSequenceLength(cl.hasOption("E"));
                dos.setExplicitSequenceLengthIfZero(!cl.hasOption("U"));
                String tsuid = cl.getOptionValue("t");
                if (cl.hasOption("d")) {
                    if (tsuid == null)
                        tsuid = TransferSyntax.ExplicitVRLittleEndian.uid();
                    dcmobj.initFileMetaInformation(tsuid);
                }
                if (cl.hasOption("a") || dcmobj.fileMetaInfo().isEmpty()) {
                    if (tsuid == null)
                        tsuid = TransferSyntax.ImplicitVRLittleEndian.uid();
                    dos.writeDataset(dcmobj, TransferSyntax.valueOf(tsuid));
                    System.out.println("Created ACR/NEMA Dump " + ofile);
                } else {
                    dos.writeDicomFile(dcmobj);
                    System.out.println("Created DICOM File " + ofile);
                }
            }
        } catch (IOException e) {
            System.err.println("xml2dcm: failed to create " + ofile + ": "
                    + e.getMessage());
            e.printStackTrace(System.err);
            //System.exit(1);

              //-------------------------------------------------------
				exit("xml2dcm: failed to create " + ofile + ": "
                    + e.getMessage(), this.get_thisObject());
              //-------------------------------------------------------
        } finally {
            CloseUtils.safeClose(dos);
        }
	    }

     //--------------------------------------------
     if(!(this.get_ObjectState().equals("failed")))
       {
	    this.set_ObjectState("successful");
	   }
	 //--------------------------------------------
     }


    private static void parseXML(String xmlFile, DicomObject dcmobj, String baseDir)
            throws FactoryConfigurationError, ParserConfigurationException,
                    SAXException, IOException {
        SAXParserFactory f = SAXParserFactory.newInstance();
        SAXParser p = f.newSAXParser();
        ContentHandlerAdapter ch = new ContentHandlerAdapter(dcmobj);
        if (xmlFile != null) {
            p.parse(new File(xmlFile), ch);
        } else if (baseDir != null ){
            String uri = "file:" + new File(baseDir, "STDIN").getAbsolutePath();
            if (File.separatorChar == '\\') {
                uri = uri.replace('\\', '/');
            }
            p.parse(System.in, ch, uri);
        } else {
            p.parse(System.in, ch);
        }
    }

    private static void loadDicomObject(File ifile, DicomObject dcmobj) throws IOException {
        DicomInputStream in = new DicomInputStream(ifile);
        try {
            in.readDicomObject(dcmobj, -1);
        } finally {
            in.close();
        }
    }

 }
