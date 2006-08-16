package loci.ome.notebook;

import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.CardLayout;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.border.*;
import loci.ome.viewer.*;
import loci.util.About;

import org.w3c.dom.*;

/**
*   MetadataNotebook.java:
*      an user-friendly application for displaying and editing OME-XML metadata.
*
*   Written by: Christopher Peterson <crpeterson2@wisc.edu>
*/

public class MetadataNotebook extends JFrame
  implements ActionListener, ItemListener, Runnable
{

  // -- Constants --

  /** Key mask for use with keyboard shortcuts on this operating system. */
  public static final int MENU_MASK =
    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();


  // -- Fields --

  protected JFileChooser chooser;
  protected MetadataPane metadata;
  protected File currentFile;
  protected JMenu tabsMenu;
  protected boolean opening;
  protected loci.ome.viewer.MetadataPane mdp;
  protected JMenuItem fileNew;

  // -- Constructor --

  public MetadataNotebook(String[] args) {
    super("OME Metadata Notebook");

    //initialize fields
    currentFile = null;
    opening = true;
    
    //give the Template.xml file to the parser to feed on
    File f = new File("Template.xml");
    TemplateParser tp = new TemplateParser(f);
    
    mdp = new loci.ome.viewer.MetadataPane();
    
    //create a MetadataPane, where most everything happens
    if( args.length > 0 ) {
			File file = null;
      try {
        file = new File(args[0]);
      }
      catch (Exception exc) {
        System.out.println("Error occured: You suck.");
      }
      currentFile = file;
      metadata = new MetadataPane(file);
    }
    else metadata = new MetadataPane();
    
    metadata.setVisible(true);
    mdp.setVisible(false);
    
    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new CardLayout());
    contentPanel.setBorder((EmptyBorder) null);
//    metadata.setBorder(new EmptyBorder(0,0,0,0));
//    mdp.setBorder(new EmptyBorder(0,0,0,0));
    contentPanel.add("notebook", metadata);
    contentPanel.add("viewer", mdp);
    setContentPane(contentPanel);

    //setup the menus on this frame
    JMenuBar menubar = new JMenuBar();
    setJMenuBar(menubar);
    JMenu file = new JMenu("File");
    menubar.add(file);
    file.setMnemonic('f');
    fileNew = new JMenuItem("New...");
    file.add(fileNew);
    fileNew.setActionCommand("new");
    fileNew.addActionListener(this);
    fileNew.setMnemonic('n');
    fileNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, MENU_MASK));
    JMenuItem fileOpen = new JMenuItem("Open");
    file.add(fileOpen);
    fileOpen.setActionCommand("open");
    fileOpen.addActionListener(this);
    fileOpen.setMnemonic('o');
    fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_MASK));
    JMenuItem fileSave = new JMenuItem("Save");
    file.add(fileSave);
    fileSave.setActionCommand("save");
    fileSave.addActionListener(this);
    fileSave.setMnemonic('s');
    fileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MASK));
    JMenuItem fileSaveAs = new JMenuItem("Save As...");
    file.add(fileSaveAs);
    fileSaveAs.setActionCommand("saveAs");
    fileSaveAs.addActionListener(this);
    fileSaveAs.setMnemonic('s');
    fileSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
      KeyEvent.CTRL_MASK|KeyEvent.SHIFT_MASK));
    JSeparator jSep = new JSeparator();
    file.add(jSep);
    JMenuItem fileExit = new JMenuItem("Exit");
    file.add(fileExit);
    fileExit.setActionCommand("exit");
    fileExit.addActionListener(this);
    fileExit.setMnemonic('x');
    fileExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, MENU_MASK));

    //setup the tab menu to reflect the top-level tab names gathered by
    //TemplateParser from the template in Template.xml
    tabsMenu = new JMenu("Tabs");
    tabsMenu.setMnemonic('b');
    menubar.add(tabsMenu);
    Element[] tabs = tp.getTabs();
    String[] tabNames = new String[tabs.length];
    for(int i = 0;i<tabs.length;i++) {
      Element e = tabs[i];
      tabNames[i] = MetadataPane.getTreePathName(e);
    }
    //call the method that changes the names in the Tabs menu
    changeTabMenu(tabNames);

    JMenu toolsMenu = new JMenu("Tools");
    toolsMenu.setMnemonic('t');
    menubar.add(toolsMenu);
    JCheckBoxMenuItem advView = new JCheckBoxMenuItem("XML View");
    advView.setSelected(false);
    toolsMenu.add(advView);
    advView.addItemListener(this);
    advView.setMnemonic('v');
    advView.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, MENU_MASK));

    JMenu help = new JMenu("Help");
    help.setMnemonic('h');
    menubar.add(help);
    JMenuItem helpAbout = new JMenuItem("About");
    help.add(helpAbout);
    helpAbout.setActionCommand("about");
    helpAbout.addActionListener(this);
    helpAbout.setMnemonic('a');
    helpAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, MENU_MASK));

    //make a filechooser to open and save our precious files
    chooser = new JFileChooser(System.getProperty("user.dir"));

    //useful frame method that handles closing of window
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    //put frame in the right place, with the right size, and make visible
    setLocation(100, 100);
    pack();
    setVisible(true);
  }


  // -- MetadataViewer API methods --

  protected void setCurrentFile(File aFile) {
    currentFile = aFile;
  }

  //opens a file, sets the title of the frame to reflect the current file
  public void openFile(File file) {
    metadata.setOMEXML(file);
    mdp.setOMEXML(file);
    setTitle("OME Metadata Notebook - " + file);
  }

  //saves to a file, sets title of frame to reflect the current file
  public void saveFile(File file) {
    metadata.saveFile(file);
  }

  //given an array of Strings of appropriate tab names, this method
  //sets up the tab menu accordingly
  public void changeTabMenu(String[] tabs) {
    tabsMenu.removeAll();
    for (int i=0; i<tabs.length; i++) {
      String thisName = tabs[i];
      JMenuItem thisTab = new JMenuItem(thisName);
      tabsMenu.add(thisTab);
      //set up shortcut keys if tabs menu has less than 11 items
      if ((i+1) < 11) thisTab.setAccelerator(KeyStroke.getKeyStroke(MetadataPane.getKey(i+1),
        InputEvent.ALT_MASK));
      Integer aInt = new Integer(i);
      thisTab.setActionCommand("tabChange" + aInt.toString());
      thisTab.addActionListener(this);
    }
  }


  // -- ActionListener API methods --

  /** Handles menu commands. */
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if ("new".equals(cmd)) {
      setTitle("OME Metadata Notebook");
      currentFile = null;
      metadata.setupTabs();
    }
    if ("open".equals(cmd)) {
      if (metadata.getState()) {
        Object[] options = {"Yes, do it!",
                    "No thanks."};
				int n = JOptionPane.showOptionDialog(this,
			    "Are you sure you want to open\n" +
			    "a new file without saving your\n" +
			    "changes to the current file?",
			    "Current File Not Saved",
			    JOptionPane.YES_NO_OPTION,
			    JOptionPane.QUESTION_MESSAGE,
			    null,     //don't use a custom Icon
			    options,  //the titles of buttons
			    options[0]); //default button title
			  if (n == JOptionPane.YES_OPTION) {
			    chooser.setDialogTitle("Open");
		      chooser.setApproveButtonText("Open");
		      chooser.setApproveButtonToolTipText("Open selected file.");
		      opening = true;
		      int rval = chooser.showOpenDialog(this);
		      if (rval == JFileChooser.APPROVE_OPTION) {
		        new Thread(this, "MetadataNotebook-Opener").start();
		      }
			  }
      }
      else {
      	chooser.setDialogTitle("Open");
		    chooser.setApproveButtonText("Open");
		    chooser.setApproveButtonToolTipText("Open selected file.");
		    opening = true;
		    int rval = chooser.showOpenDialog(this);
		    if (rval == JFileChooser.APPROVE_OPTION) {
          new Thread(this, "MetadataNotebook-Opener").start();
	      }
      }
    }
    else if ("saveAs".equals(cmd) ||
      ("save".equals(cmd) && currentFile == null))
    {
      opening = false;
      chooser.setDialogTitle("Save");
      chooser.setApproveButtonText("Save");
      chooser.setApproveButtonToolTipText("Save to selected file.");
      int rval = chooser.showOpenDialog(this);
      if (rval == JFileChooser.APPROVE_OPTION) {
        new Thread(this, "MetadataNotebook-Saver").start();
        metadata.stateChanged(false);
      }
    }
    else if ("save".equals(cmd) && currentFile != null) {
      saveFile(currentFile);
      metadata.stateChanged(false);
    }
    else if ("exit".equals(cmd)) {
          if (metadata.getState()) {
        Object[] options = {"Yes, exit!",
                    "No thanks."};
				int n = JOptionPane.showOptionDialog(this,
			    "Are you sure you want to exit without\n" +
			    "saving your changes to the current file?",
			    "Current File Not Saved",
			    JOptionPane.YES_NO_OPTION,
			    JOptionPane.QUESTION_MESSAGE,
			    null,     //don't use a custom Icon
			    options,  //the titles of buttons
			    options[0]); //default button title
			  if (n == JOptionPane.YES_OPTION) {
			    System.exit(0);
			  }
      }
      else {
				System.exit(0);
      }
    }
    else if ("about".equals(cmd)) About.show();
    else if(cmd.startsWith("tabChange")) {
      metadata.tabChange( Integer.parseInt(cmd.substring(9)) );
    }
  }

  public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
		  metadata.setVisible(false);
		  tabsMenu.setEnabled(false);
		  fileNew.setEnabled(false);
			mdp.setVisible(true);
      mdp.setOMEXML(metadata.getRoot());
    }
    else {
      mdp.setVisible(false);
      tabsMenu.setEnabled(true);
		  fileNew.setEnabled(true);
			metadata.setVisible(true);
	  }
  }

  // -- Runnable API methods --

  /** Opens a file in a separate thread. */
  public void run() {
    wait(true);
    currentFile = chooser.getSelectedFile();
    if (opening) openFile(currentFile);
    else saveFile(currentFile);
    wait(false);
  }


  // -- Helper methods --

  /** Toggles wait cursor. */
  protected void wait(boolean wait) {
    setCursor(wait ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
  }


  // -- Main method --

  public static void main(String[] args) { new MetadataNotebook(args); }

}
