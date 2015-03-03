/* License:
 * Open source ftw. I doubt anybody would actually like to use this file.
 * But if you want, use it, change it or share it under any license you like.
 */

package cz.autoclient.autoclick;
//Comment this out if you don't have the library
import com.sun.jna.platform.win32.WinDef;
//Used for exporting rect class as normal rectangle
import java.awt.Rectangle;
import java.util.ArrayList;

/**
 *
 * @author Jakub Mareda
 */
public class Rect {
  public final int width;
  public final int height;
  public final int top;
  public final int bottom;
  public final int left;
  public final int right;
  /** The parameters go clockwise - top, right, bottom, left
   * @param t top
   * @param r right
   * @param b bottom
   * @param l  left**/
  public Rect(int t, int r, int b, int l) {
     top = t;
     bottom = b;
     right = r;
     left = l;
     width = r-l;
     height = b-t;
  }
 /** Can be used to define a point. In this case, width and height will be zero.
   * @param x x offset of the point
   * @param y y offset of the point**/
  public Rect(int x, int y) {
     top = bottom = y;
     left = right = x;
     width = height = 0;
  }
  /** Using WinDef.RECT from com.sun.jna.platform.win32.WinDef. Comment this out for 
   * if this library is not present. Java doesn't provide smarter approach.
   * @param rect WinDef rect. Can be obtained for example by sirius.core.user32Ext.GetClientRect
   **/
  public Rect(WinDef.RECT rect) {
     top = rect.top;
     bottom = rect.bottom;
     right = rect.right;
     left = rect.left;
     width = right-left;
     height = bottom-top;
  }
  /** Define by starting offset (top, left) and width and height.
   * 
   * @param t top offset (y axis)
   * @param l left offset (x axis)
   * @param w width (x axis)
   * @param h height (y axis)
   * @return 
   */
  public static Rect byWidthHeight(int t, int l, int w, int h) {
    return new Rect(t, l+w, t+h, l);
  }
  /** Get the middle point of the rectangle.
   * @return Rect of zero size that is in the middle of this rect.
   */
  public Rect middle() {
      return new Rect(width/2+left, height/2+top); 
  }
  /** Get rect that is at same position but rescaled 
   * @param scale float parameter of the scale ratio, where 1 is equal rectangle
   * @return rescaled rectangle
   */
  public Rect rescaleSize(float size) {
    return new Rect(top, (int)(right*size), (int)(bottom*size), left);
    
  }
  
  /** Check if two rectangles overlap (have nonempty intersection)
   * @param a forst rectangle
   * @param b second rectangle
   * @return true if they overlap
   */
  public static boolean intersection(Rect a, Rect b) {
    return a.right > b.left &&
    a.left < b.right &&
    a.bottom > b.top &&
    a.top < b.bottom;
  }
  
  public static ArrayList<ArrayList<Rect>> groupOverlapingRects(Rect rects[], boolean return_loners) {
    Rect currentRect;
    ArrayList<Rect> currentGroup;
    ArrayList<ArrayList<Rect>> results = new ArrayList<>();
    int results_iterator = -1;
    //length-1 because the last one never needs to be checked

    for(int i=0, l=rects.length-1; i<l; i++) {
      currentRect = rects[i];
      currentGroup = null;
      for(int j=i+1; j<=l; j++) {
        if(intersection(currentRect, rects[j])) {
          if(currentGroup==null) {
            currentGroup = new ArrayList<>();
            currentGroup.add(currentRect);
          }
          currentGroup.add(rects[j]);
        }
      }
      if(currentGroup==null&&return_loners) {
        currentGroup = new ArrayList<>();
        currentGroup.add(currentRect);
        //System.out.println(i+": Created the entry anyway, because even lonely rect has a right to be returned.");
      }
      if(currentGroup!=null) {
        //Check if the current group isn't fully contained within the previous group
        //this means, if all current groups element are the same as in the previous group, do not include it
        if(results_iterator<0 || arrayMatches(currentGroup, results.get(results_iterator))<currentGroup.size()) {
          results.add(currentGroup);
          results_iterator++;
        }
        /*else {
          System.out.println(i+": Current entry is ignored because it's already included in another group.");
          System.out.println(arrayInArray(currentGroup,  results));
        }*/
      }
    }
    //Try to add the last rectangle if it isn't in any former group
      //Unfinished, I have bigger problems now
    
   
    return results; 
  }
  public static ArrayList<ArrayList<Rect>> groupOverlapingRects(Rect rects[]) {
    return groupOverlapingRects(rects, true);
  }
  private static int arrayMatches(ArrayList<Rect> small, ArrayList<Rect> big) {
    int matches = 0;
    Rect current;
    for(int i=0,l=small.size(); i<l; i++) {
      current = small.get(i);
      for(int j=0,k=big.size(); j<k; j++) {
        if(current==big.get(j)) {
          matches++;
          break;
        }
      }
    }
    return matches;
  }
  private static int arrayInArray(ArrayList<Rect> small, ArrayList<ArrayList<Rect>> big) {
    ArrayList<Rect> current;
    int ss = small.size();
    
    for(int i=0,l=big.size(); i<l; i++) {
      //If all small array entries can be found in big sub array
      if(arrayMatches(small, big.get(i))>=ss) {
        //Get the position of that array
        return i;
      }
    }
    return -1;
  }
  @Override
  public String toString() {
    return "autoclick.Rect["+top+", "+right+", "+bottom+", "+left+"] ("+width+" x "+height+")"; 
  }
  
  public String toCode() {
    return "new Rect("+top+", "+right+", "+bottom+", "+left+");";
  }
  /** Get java.awt.Rectangle equal to this rectangle. Intelligent language 
   *  would allow me to override (Rectangle). But Java really sucks.
   * @return java.awt.Rectangle of the same size and position as this rectangle.
   */
  public Rectangle toStdRectangle() {
    return new Rectangle(top, left, width, height); 
  }
}