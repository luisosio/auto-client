/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.autoclient.PVP_net;

import cz.autoclient.settings.Settings;

/**
 * Holds setting names and default values. This allows us to easily refactor settings whenever we want.
 * @author Jakub
 */
public enum Setnames {
  TEAMBUILDER_ENABLED("tb_enabled", false),
  //Whether the program should automatically start a game if possible
  TEAMBUILDER_AUTOSTART_ENABLED("tb_cap_autostart_enabled", false),
  INVITE_ENABLED("invite_enabled", false),
  TRAY_ICON_ENABLED("tray_enabled", true),
  TRAY_ICON_MINIMIZE("tray_minimize", false),
  
  BLIND_SUMMONER1("sumspell1", null),
  BLIND_SUMMONER2("sumspell2", null),
  BLIND_CHAMP_NAME(""),
  BLIND_CALL_TEXT(""),
  
  BLIND_MASTERY("masterypg", 0),
  BLIND_RUNE("runepg", 0),
  /**NOTFICATONS**/
  NOTIF_MENU_TB_GROUP_JOINED(false),
  
  //Notify when everybody is ready and the game can start (or was started, if auto start is enabled)
  NOTIF_MENU_TB_READY_TO_START(true),
  NOTIF_MENU_TB_PLAYER_JOINED(false),
  
  NOTIF_MENU_BLIND_IN_LOBBY(false),

  AM_SAY(),
  AUTO_QUEUE_ENABLED(false),
  
  
  ;
  
  public final String name;
  public final Object default_val;
  Setnames(String n, Object d) {
    name = n;
    default_val = d;    
  }
  Setnames(Object d) {
    name = this.name().toLowerCase();
    default_val = d;    
  }
  Setnames() {
    name = this.name().toLowerCase();
    default_val = null;    
  }
  public static void setDefaults(Settings settings) {
    for(Setnames set : Setnames.values()) {
      settings.setSettingDefault(set.name, set.default_val);
    }
  }
}
