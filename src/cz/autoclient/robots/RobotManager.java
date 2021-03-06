/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.autoclient.robots;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jakub
 */
public class RobotManager extends Thread {
  protected final List<Robot> robots = new ArrayList<>();
  private final int checkInterval;
  public RobotManager(int checkInterval) {
    this.checkInterval = checkInterval;
    setDaemon(true);
    setPriority(Thread.MIN_PRIORITY);
  }
  public RobotManager() {
    this(800);
  }
  
  @Override
  public final synchronized void start() {
    super.start();
  }
  
  public void addRobot(Robot rur) {
    synchronized (robots) {
      robots.add(rur);
      //If the robot array was empty, notify the main thread that
      //it's not empty anymore
      if(robots.size()==1)
        robots.notify(); 
    }
  }
  public void removeRobot(Robot rur) {
    synchronized (robots) {
      if(rur.isRunning())
        rur.stop();
      robots.remove(rur);
    }
  }
  
  @Override
  public final void run() {
    int offset = -1;
    int individual_delay;
    int size;
    Robot rur = null;
    try {
      while(!interrupted()) {
        //System.out.println("Entering robot synchronized thread.");
        synchronized (robots) {
          size = robots.size();
          if(size==0) {
            individual_delay = checkInterval;
            System.out.println("No robots. Waiting for robots...");
            robots.wait();
            System.out.println("Finally got some robots!");
          }
          else {
            individual_delay = checkInterval/size;
            offset++;
            if(offset>=size)
              offset = 0;
            rur = robots.get(offset);
          }
        }
        //System.out.println("  Current sleep interval: "+individual_delay);
        //System.out.println("  Robot: "+(rur!=null?rur.getClass().getName():"null"));
        //System.out.println("  Robots: "+(size));
        if(rur!=null) {
          if(!rur.isRunning()) {
            if(rur.canRun()) {
              //System.out.println("    "+rur.getClass().getName()+" started.");
              rur.start();
            }
          }
          /*else {
            System.out.println("    "+rur.getClass().getName()+" running.");
          }*/
        }
        rur = null;
        sleep(individual_delay);

        //yield();
      }
    }
    catch(InterruptedException e) {
     
    }
    catch(Exception e) {
      run(); 
    }
  }
}
