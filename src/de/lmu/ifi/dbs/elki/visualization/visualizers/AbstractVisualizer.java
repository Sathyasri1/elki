package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.utilities.AnyMap;

/**
 * Abstract superclass for Visualizers.
 * 
 * @author Remigius Wojdanowski
 */
public abstract class AbstractVisualizer extends AbstractLoggable implements Visualizer {
  /**
   * Visualizer context to use
   */
  protected VisualizerContext context;

  /**
   * Meta data storage
   */
  protected AnyMap<String> metadata;

  /**
   * Constructor with default level.
   */
  protected AbstractVisualizer() {
    this.metadata = new AnyMap<String>();
    this.metadata.put(Visualizer.META_LEVEL, Visualizer.LEVEL_STATIC);
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param name a short name characterizing this Visualizer
   * @param context Visualization context
   */
  protected void init(String name, VisualizerContext context) {
    this.metadata.put(Visualizer.META_NAME, name);
    this.context = context;
  }
  
  /**
   * Convenience method to update the visualizer level.
   * 
   * @param level new level.
   */
  protected void setLevel(int level) {
    this.metadata.put(Visualizer.META_LEVEL, level);
  }

  @Override
  public AnyMap<String> getMetadata() {
    return metadata;
  }
}
