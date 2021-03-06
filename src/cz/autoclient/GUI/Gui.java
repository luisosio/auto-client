package cz.autoclient.GUI;

import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import cz.autoclient.GUI.champion.ConfigurationManager;
import cz.autoclient.GUI.tabs.TabbedWindow;
import cz.autoclient.GUI.tabs.FieldDef;
import cz.autoclient.GUI.notifications.Notification;
import cz.autoclient.GUI.notifications.NotificationTrayBaloon;
import cz.autoclient.GUI.notifications.Notifications;
import cz.autoclient.GUI.passive_automation.PAMenu;
import cz.autoclient.GUI.summoner_spells.ButtonSummonerSpellMaster;
import cz.autoclient.GUI.tabs.MultiFieldDef;
import cz.autoclient.Main;
import cz.autoclient.PVP_net.Constants;
import cz.autoclient.PVP_net.Setnames;
import cz.autoclient.PVP_net.SummonerSpell;
import cz.autoclient.settings.Settings;
import cz.autoclient.dllinjection.DLLInjector;
import cz.autoclient.dllinjection.InjectionResult;
import cz.autoclient.robots.AutoQueueBot;
import cz.autoclient.robots.LaunchBot;
import cz.autoclient.robots.RobotManager;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
 
 public class Gui
   extends JFrame
 {
   private Main ac;
   private StateGuard guard;
   private Settings settings;
   private Notifications notifications = new Notifications();
   //Is PVP.net window available?
   private boolean pvp_net_window = false;
   //Was our DLL loaded by the user?
   private boolean dll_loaded = false;
   
   private JMenuItem manu_threadcontrol_pretend_accepted;
   private JMenuItem menu_dll_additions;
   private JCheckBoxMenuItem menu_tray_enabled;
   private JCheckBoxMenuItem menu_tray_minimize;
   
   private TrayIcon tray_icon;
   //Remember whether tray icon has been added or not
   private boolean tray_added = false;
   private final SystemTray tray;
   
   RobotManager robots;
   
   /**
    * Is set to true if the AutoClient anti-annoyance functions are in place
    */
   private boolean anoyance_disabled = false;
  private ConfigurationManager champ_config;
   
   
   public String[] getSelectedMode()
   {
     return new String[0];//(String[]) (selected != null ? selected : "");
   }
   
   public Gui(Main acmain, final Settings settings)
   {
     this.ac = acmain;
     this.settings = settings;
     
     setIconImage(ImageResources.ICON.getImage());
     //The tray is final so it must be initialised in constructor
     if(SystemTray.isSupported())
       tray = SystemTray.getSystemTray();
     else
       tray = null;
     
     robots = new RobotManager(900);
     initMenu();
     initComponents();
     
     

     //There is many factors that determine whether the icon will be shown
     // it must be both supported and enabled
     // 
     displayTrayEnabled();
     
     
     guard = new StateGuard(this.ac, this);
     
     //After creating the GUI, render the settings
     SwingUtilities.invokeLater(new Runnable()
     {
       @Override
       public void run()
       {
        //Display settings:
        settings.displaySettingsOnBoundFields();
        //Start passive automation
        robots.start();
       }
     });
     //notification(Notification.Def.TB_GAME_CAN_START);
     this.addWindowListener(new WindowAdapter()
     {
        @Override
        public void windowOpened(WindowEvent event)
        {
          System.out.println("Window opened, starting GUI guard.");
          guard.start();
        }
        @Override
        public void windowClosing(WindowEvent event)
        {
          //Stop updating GUI
          guard.interrupt();
          //Save settings
          if(settings==null) {
            System.out.println("Settings is null!");
            return;
          }
          try {
            settings.loadSettingsFromBoundFields();
            settings.saveToFile(Main.SETTINGS_FILE, false);
          }
          catch(IOException e) {}
          //Hide tray icon
          if(canTray())
            tray.remove(tray_icon);
        }
        @Override
        public void windowClosed(WindowEvent event)
        {
          System.out.println("This function is never called.");
        }
        @Override
        public void windowIconified(WindowEvent event) {
          if(Gui.this.canTray()) {
            if(settings.getBoolean(Setnames.TRAY_ICON_MINIMIZE.name, false)) {
              //Gui.this.setType(javax.swing.JFrame.Type.UTILITY);
              Gui.this.setState(Frame.ICONIFIED);
              Gui.this.setVisible(false);
            }            
          }
        }
     });

     setSize(500, 300);
   }
   
   public void displayToolAction(boolean state) {
     toggleButton1.setText(state ? "Stop" : "Start");
     toggleButton1.setSelected(state);
     toggleButton1.setBackground(state? Color.RED:null);
     //Enable/disable thread control
     manu_threadcontrol_pretend_accepted.setEnabled(state);
     
     if(!state) {
       setTitle("Stopped."); 
     }
   }
   public void displayToolAction() {
     displayToolAction(ac.ToolRunning());
   }
   
   public void displayClientAvailable(boolean available) {
    toggleButton1.setEnabled(available);
    pvp_net_window = available;
    if(available) {
      toggleButton1.setToolTipText(Text.TOGGLE_BUTTON_TITLE_ENABLED.text);
      displayDllStatus(dll_loaded);
    }
    else {
      toggleButton1.setToolTipText(Text.TOGGLE_BUTTON_TITLE_DISABLED.text);
      menu_dll_additions.setEnabled(false);
    } 
   }
   public void displayTrayEnabled(boolean enabled, boolean minimize, boolean do_not_change_state) {
     if(tray==null || !SystemTray.isSupported()) {
       enabled = minimize = false;
     }
     if(!do_not_change_state) {
       menu_tray_enabled.setState(enabled);         
       menu_tray_minimize.setState(enabled&&minimize); 
     }
     menu_tray_minimize.setEnabled(enabled);
     if(enabled) {
       showTrayIcon(true);
     }
     else {
       showTrayIcon(false);;
     }
   }
   public void displayTrayEnabled(boolean enabled, boolean minimize) {
     displayTrayEnabled(enabled, minimize, false);
   }
   public void displayTrayEnabled(boolean enabled) {
     displayTrayEnabled(enabled, settings.getBoolean(Setnames.TRAY_ICON_MINIMIZE.name, (Boolean)Setnames.TRAY_ICON_MINIMIZE.default_val), false);
   }

   public void displayTrayEnabled() {
     displayTrayEnabled(
         settings.getBoolean(Setnames.TRAY_ICON_ENABLED.name, (Boolean)Setnames.TRAY_ICON_ENABLED.default_val),
         settings.getBoolean(Setnames.TRAY_ICON_MINIMIZE.name, (Boolean)Setnames.TRAY_ICON_MINIMIZE.default_val),
         false);
   }
   
   public void displayDllStatus(boolean loaded) {
     dll_loaded = loaded;
     if(loaded) {
       menu_dll_additions.setText(Text.MENU_DLL_LOADED.text);
       menu_dll_additions.setEnabled(false);
     }
     else {
       menu_dll_additions.setText(Text.MENU_DLL_LOAD.text);
       if(DLLInjector.available()) {
         menu_dll_additions.setEnabled(pvp_net_window);
         menu_dll_additions.setToolTipText(Text.MENU_DLL_TITLE.text);
       }
       else {
         menu_dll_additions.setToolTipText(Text.MENU_DLL_TITLE_UNAVAILABLE.text);
         menu_dll_additions.setEnabled(false);
       }
     }
   }

   
   private boolean ToolAction()
   {
     boolean toolState = ac.ToolRunning();
     displayToolAction(!toolState);
     
     if (!toolState) {
       ac.StartTool();
     } else {
       ac.StopTool();
     }
     return !toolState;
   }

   private boolean ToolAction(ActionEvent e) {
     return ToolAction();
   }
 
   
   public JToggleButton getToggleButton1()
   {
     return this.toggleButton1;
   }
  
   private void DelaySelected(ActionEvent e)
   {
     this.chatDialog.setVisible(true);
   }
   
   private void ChatOKClicked(ActionEvent e)
   {
     this.chatDialog.setVisible(false);
   }
   public void notification(Notification.Def... names) {
     notifications.notification(names);
   }
   
   public int getDelay()
   {
     return (int)(((Double)this.spinner1.getValue()).doubleValue() * 1000.0D);
   }
   private void initTrayIcon() {
     if(tray_icon!=null)
       return;
     if (SystemTray.isSupported() && settings.getBoolean(Setnames.TRAY_ICON_ENABLED.name, true)) {
       tray_icon = new TrayIcon(ImageResources.ICON.getImage());
       tray_icon.setImageAutoSize(true);
       tray_icon.addMouseListener(new MouseListener() {
         @Override
         public void mouseClicked( MouseEvent e ) {
           Gui.this.setVisible(true);
           Gui.this.setState (Frame.NORMAL);
           //Gui.this.setType(java.awt.Window.Type.NORMAL);
         }
         @Override
         public void mousePressed( MouseEvent e ) {}
         @Override
         public void mouseExited(MouseEvent e) {}
         @Override
         public void mouseReleased(MouseEvent e) {}
         @Override
         public void mouseEntered(MouseEvent e) {}
       });
     }
   }
   private void showTrayIcon(boolean state) {
     //Just make sure tray icon was created
     initTrayIcon();
     //If tray is not supported, return
     if(!canTray())
       return;
     if(state && !tray_added) {
       try {
         tray.add(tray_icon);
       } catch (AWTException e) {
         //No tray icon
         tray_icon = null;
       }
       if(tray_icon!=null)
         tray_added = true;
     }
     else if(!state && tray_added) {
       tray.remove(tray_icon); 
       tray_added = false;
     }
   }
   private boolean canTray() {
     return tray!=null && tray_icon!=null; 
   }
   private void initMenu() {
     menuBar1 = new JMenuBar();
     //======== menu1 ========
     {
       menu1 = new JMenu();

       menu1.setText("Settings");

       //---- menuItem1 ----
       menuItem1 = new JMenuItem();
       menuItem1.setText("Set Chat Delay");
       menuItem1.addActionListener(new ActionListener() {
           @Override
           public void actionPerformed(ActionEvent e) {
               DelaySelected(e);
           }
       });
       menu1.add(menuItem1);

       final JMenuItem menuItem2 = new JMenuItem();
       menuItem2.addActionListener(new ActionListener() {
           @Override
           public void actionPerformed(ActionEvent e) {
               menuItem2.setEnabled(false);
               menuItem2.setText("Injecting...");

               DLLInjector.inject(new InjectionResult() {
                   @Override
                   public void run(boolean result, String fail_reason) {
                      if(result)
                        menuItem2.setText(Text.MENU_DLL_LOADED.text);
                      else {
                        menuItem2.setEnabled(true);
                        menuItem2.setText("Injection failed: "+fail_reason);
                      }
                   }
               });
           }
       });
       menu1.add(menuItem2);
       menu_dll_additions = menuItem2;
       //Update dll aditions status
       displayDllStatus(false);

       manu_threadcontrol_pretend_accepted = new JMenuItem();
       manu_threadcontrol_pretend_accepted.setText("Detect game lobby");
       manu_threadcontrol_pretend_accepted.setToolTipText("If the lobby has been already invoked, skip accept phase.");
       manu_threadcontrol_pretend_accepted.setEnabled(false);
       manu_threadcontrol_pretend_accepted.addActionListener(new ActionListener() {
           @Override
           public void actionPerformed(ActionEvent e) {
             Gui.this.ac.ac.simulateAccepted();
           }
       });
       menu1.add(manu_threadcontrol_pretend_accepted);
     }
     menuBar1.add(menu1);
     //======== menu2 ========
     {
       JMenu menu = new JMenu();
       menu.setText("Display");

       menu_tray_enabled = new JCheckBoxMenuItem("Show in system tray");
       menu_tray_enabled.addActionListener(new ActionListener() {
           @Override
           public void actionPerformed(ActionEvent e) {
             boolean enabled = menu_tray_enabled.getState();
             boolean minimize = menu_tray_minimize.getState();
             if(!enabled)
               menu_tray_minimize.setState(false);
             displayTrayEnabled(enabled, minimize, true);
             if(!enabled && minimize) {
               //Send event to inform other checkbox that it's been disabled
               e.setSource(menu_tray_minimize);
               for(ActionListener a: menu_tray_minimize.getActionListeners()) {
                 a.actionPerformed(e);
               }
             }
           }
       });
       menu.add(menu_tray_enabled);
       settings.bindToInput(Setnames.TRAY_ICON_ENABLED.name,menu_tray_enabled, true);


       menu_tray_minimize = new JCheckBoxMenuItem("Minimize to tray");
       menu_tray_minimize.setToolTipText("When minimized, the application will disappear from task bar.");
       menu_tray_minimize.addActionListener(new ActionListener() {
           @Override
           public void actionPerformed(ActionEvent e) {
             displayTrayEnabled(menu_tray_enabled.getState(), menu_tray_minimize.getState(), true);
           }
       });
       menu.add(menu_tray_minimize);
       settings.bindToInput(Setnames.TRAY_ICON_MINIMIZE.name, menu_tray_minimize, true);
       //Add this menu to the bar
       menuBar1.add(menu);
     }
     //======== Notifications menu ========
     initTrayIcon() ;
     {
       JMenu menu = new JMenu();
       menu.setText("Notifications");
       Notification.Def.createAll(notifications, NotificationTrayBaloon.class, settings, tray_icon);
       //======== Tray Notifications menu ========
       {
         JMenu tray_notifs = new JMenu();
         tray_notifs.setText("Tray bubble");
         notifications.addToJMenu(tray_notifs, NotificationTrayBaloon.class);
         tray_notifs.setEnabled(canTray());
         menu.add(tray_notifs);
       }
       menuBar1.add(menu);
     }
     //======== Passive automation menu ========
     //if(false)
     {
        JMenu menu = new JMenu();
        menu.setText("Passive Automation");
        JMenuItem disableAll = new JMenuItem("Disable all");
        
        //======== Auto Launch ========
        {
          PAMenu auto_launch = new PAMenu(new LaunchBot(), settings, "Auto launch");
          auto_launch.setRobots(robots);
          menu.add(auto_launch.root);
        }
        menuBar1.add(menu);
      }
      setJMenuBar(menuBar1);

      //---- toggleButton1 ----
      toggleButton1 = new JToggleButton();
      toggleButton1.setText("Start");
      toggleButton1.setToolTipText(Text.TOGGLE_BUTTON_TITLE_DISABLED.text);
      toggleButton1.setEnabled(false);
      toggleButton1.setFocusable(true);
      toggleButton1.setFocusPainted(false);
      toggleButton1.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            ToolAction();
          }
      });
      menuBar1.add(toggleButton1);
    }
    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - Jakub Mareda
        

        
        chatDialog = new Dialog(this);
        label1 = new JLabel();
        spinner1 = new JSpinner();
        button1 = new JButton();

        //======== this ========
        setTitle("Application - stopped");
        Container contentPane = getContentPane();
        contentPane.setLayout(null);
        createTabs(contentPane);

 
        //contentPane.add(toggleButton1);
        //toggleButton1.setBounds(142, 151, 69, 19);

        { // compute preferred size
            Dimension preferredSize = new Dimension();
            for(int i = 0; i < contentPane.getComponentCount(); i++) {
                Rectangle bounds = contentPane.getComponent(i).getBounds();
                preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
            }
            Insets insets = contentPane.getInsets();
            preferredSize.width += insets.right;
            preferredSize.height += insets.bottom;
            contentPane.setMinimumSize(preferredSize);
            contentPane.setPreferredSize(preferredSize);
        }
        pack();
        setLocationRelativeTo(getOwner());

        //======== chatDialog ========
        {
            chatDialog.setTitle("Chat Delay");
            chatDialog.setResizable(false);

            //---- label1 ----
            label1.setText("Enter a delay:");
            label1.setToolTipText("Sets an additional delay before tool types your message. (Seconds)");

            //---- spinner1 ----
            spinner1.setModel(new SpinnerNumberModel(0.0, 0.0, 5.0, 0.1));

            //---- button1 ----
            button1.setText("OK");
            button1.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ChatOKClicked(e);
                }
            });

            GroupLayout chatDialogLayout = new GroupLayout(chatDialog);
            chatDialog.setLayout(chatDialogLayout);
            chatDialogLayout.setHorizontalGroup(
                chatDialogLayout.createParallelGroup()
                    .addGroup(chatDialogLayout.createSequentialGroup()
                        .addGap(54, 54, 54)
                        .addComponent(button1)
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(chatDialogLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(label1)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(spinner1, GroupLayout.PREFERRED_SIZE, 47, GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
            );
            chatDialogLayout.setVerticalGroup(
                chatDialogLayout.createParallelGroup()
                    .addGroup(chatDialogLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(chatDialogLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(label1)
                            .addComponent(spinner1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(button1)
                        .addContainerGap())
            );
            chatDialog.pack();
            chatDialog.setLocationRelativeTo(chatDialog.getOwner());
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
        

    }
    //http://docs.oracle.com/javase/tutorial/uiswing/examples/components/TabbedPaneDemoProject/src/components/TabbedPaneDemo.java
    public void createTabs(Container pane) {
        pane.setLayout(new GridLayout(1, 1));
        TabbedWindow win = new TabbedWindow();
        
        win.newTab("Blind pick lobby", "Lobby where lane is called and champion is picked");
        
        
        MultiFieldDef multifield = new MultiFieldDef("Champion:");
        JComboBox champion = new JComboBox();        
        champion.enableInputMethods(true);
        champion.setEditable(true);
        initChampField(champion);
        
        multifield.addField(/*new JTextField()*/champion, settings, Setnames.BLIND_CHAMP_NAME);
        
        champ_config = new ConfigurationManager(champion, settings);
        multifield.addField(champ_config.save, null, null);
        multifield.addField(champ_config.delete, null, null);
    
        
        multifield.packWeighted(0.5,0,0);
        win.addLine(multifield);
        
        FieldDef field = new FieldDef("Call text:", "Enter text to say after entering lobby.", Setnames.BLIND_CALL_TEXT.name);
        field.addField(new JTextField());
        field.attachToSettings(settings);
        win.addLine(field);
        
        multifield = new MultiFieldDef("Summoner spells:");
        ButtonSummonerSpellMaster spell1 = new ButtonSummonerSpellMaster(SummonerSpell.Ignite, settings);
        ButtonSummonerSpellMaster spell2 = new ButtonSummonerSpellMaster(SummonerSpell.Ignite, settings); 
        spell1.setTwin(spell2);
        multifield.addField(spell1, settings, Setnames.BLIND_SUMMONER1);
        multifield.addField(spell2, settings, Setnames.BLIND_SUMMONER2);
        win.addLine(multifield);
        
        field = new FieldDef("Mastery page (0=ignore):", "Index of mastery page to be selected:", Setnames.BLIND_MASTERY.name);
        JSpinner masteries = new JSpinner();
        masteries.setModel(new SpinnerNumberModel(0.0, 0.0, 20.0, 1.0));
        field.addField(masteries);
        field.attachToSettings(settings);
        win.addLine(field);
        
        field = new FieldDef("Rune page (0=ignore):", "Index of rune page to be selected:", Setnames.BLIND_RUNE.name);
        JSpinner runes = new JSpinner();
        runes.setModel(new SpinnerNumberModel(0.0, 0.0, 20.0, 1.0));
        field.addField(runes);
        field.attachToSettings(settings);
        win.addLine(field);

        
        win.newTab("Team builder", "All teambuilder automation");
        
        field = new FieldDef("Enabled:", "Enable or disable this function.", Setnames.TEAMBUILDER_ENABLED.name);
        field.addField(new JCheckBox());
        field.attachToSettings(settings);
        win.addLine(field);
        
        field = new FieldDef("Auto-Start:", "Start a game when everybody is ready.", Setnames.TEAMBUILDER_AUTOSTART_ENABLED.name);
        field.addField(new JCheckBox());
        field.attachToSettings(settings);
        win.addLine(field);
        
        field = new FieldDef("Greet new player:", "As captain, you'll automatically call this to newcomers", "tb_cap_greet");
        field.addField(new JTextField());
        field.attachToSettings(settings);
        win.addLine(field);
        
        field = new FieldDef("Lock call:", "Sentence you call when everybody should lock in.", "tb_cap_lock");
        field.addField(new JTextField());
        field.attachToSettings(settings);
        win.addLine(field);
        
        
        win.newTab("Invite friends", "Start automatically when everybody accepts.");
        
        field = new FieldDef("Auto start:", "Enable or disable this function.", Setnames.INVITE_ENABLED.name);
        field.addField(new JCheckBox());
        field.attachToSettings(settings);
        win.addLine(field);

        pane.add(win.container);
        win.close();        
   }
   protected void initChampField(final JComboBox field) {
     SwingUtilities.invokeLater(new Runnable()
     {
       @Override
       public void run()
       {
         //Object[] elements = new Object[] {"Cat", "Dog", "Lion", "Mouse"};
         String[] elements = Constants.lolData.getChampions().allNames();
         //System.out.println("Champions loaded: "+elements.length);
         AutoCompleteSupport.install(field, GlazedLists.eventListOf((Object[])elements));
       }
     });
   }
   protected static JPanel newLine() {
     JPanel line = new JPanel();
     line.setLayout(new GridLayout(1, 1));
     line.setBorder(BorderFactory.createLineBorder(Color.red));
     return line;
   }
   protected JPanel textLine(String label, String title, String setting) {
     JPanel line = newLine();
     line.add(makeTextPanel("Champion:"));
     JTextField field = new JTextField();
     field.setToolTipText("Enter champion name");
     if(setting!=null) {
       settings.bindToInput(setting, field);
     }
     line.add(field);
     return line;
   }
   protected static void endLine(ParallelGroup h, SequentialGroup v, JComponent line) {
     h.addComponent(line);
     v.addComponent(line, GroupLayout.PREFERRED_SIZE,
            GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);
     v.addGap(10);
   }
    protected static JComponent makeTextPanel(String text) {
        //JPanel panel = new JPanel(false);
        JLabel filler = new JLabel(text);
        filler.setHorizontalAlignment(JLabel.CENTER);
        return filler;
        //panel.setLayout(new GridLayout(1, 1));
        //panel.add(filler);
        //return panel;
    }
    private static void debugInspectElement(Container c, int recur) {
      Component[] comps = c.getComponents();
      if(recur==0)
        System.out.println("Inspecting "+c.getClass().getName()+": ");
      for(Component comp:comps) {
        System.out.print("  ");
        if(recur>0) {
          if(comp==comps[comps.length-1])
            System.out.print("╚");
          else
            System.out.print("╠");
          for(int i=0;i<recur; i++)
            System.out.print(" ");
        }
        System.out.println(comp.getClass().getName());
        if(comp instanceof Container) {
          //System.out.println(" );
          debugInspectElement((Container)comp, recur+2); 
        }
      }
    }
    public static void debugInspectElement(Container c) {
      debugInspectElement(c, 0);
    }
    
    
    
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - Jakub Mareda
    private JMenuBar menuBar1;
    private JMenu menu1;
    private JMenuItem menuItem1;



    //private JTextField champField;
    //private JTextField textField2;
    private JToggleButton toggleButton1;
    private Dialog chatDialog;
    private JLabel label1;
    private JSpinner spinner1;
    private JButton button1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
 }

