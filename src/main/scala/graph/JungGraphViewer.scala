package graph

import edu.uci.ics.jung.algorithms.layout.Layout
import edu.uci.ics.jung.algorithms.layout.util.Relaxer
import edu.uci.ics.jung.algorithms.layout.util.VisRunner
import edu.uci.ics.jung.algorithms.util.IterativeContext
import edu.uci.ics.jung.graph.Graph
import java.awt.Dimension
import javafx.scene.layout.Region
import javafx.scene.shape._


class JungGraphViewer[V, E](layout: Layout[V, E]) extends Region {
//  private var relaxer: Relaxer = null
//  private val CIRCLE_SIZE = 25.0
//
//  override protected def layoutChildren(): Unit = {
//    super.layoutChildren()
//    layout.setSize(new Dimension(widthProperty.intValue, heightProperty.intValue))
//    // relax the layout
//    if (relaxer != null) {
//      relaxer.stop()
//      relaxer = null
//    }
//    if (layout.isInstanceOf[IterativeContext]) {
//      layout.initialize()
//      if (relaxer == null) {
//        relaxer = new VisRunner(this.layout.asInstanceOf[IterativeContext])
//        relaxer.prerelax()
//        relaxer.relax()
//      }
//    }
//    val graph = layout.getGraph
//    // draw the vertices in the graph
//    for (v <- graph.getVertices) { // Get the position of the vertex
//      val p: java.awt.geom.Point2D = layout.   //.transform(v)
//      // draw the vertex as a circle
//      val circle = CircleBuilder.create.centerX(p.getX).centerY(p.getY).radius(CIRCLE_SIZE).build
//      // add it to the group, so it is shown on screen
//      this.getChildren.add(circle)
//    }
//    // draw the edges
//    import scala.collection.JavaConversions._
//    for (e <- graph.getEdges) { // get the end points of the edge
//      val endpoints = graph.getEndpoints(e)
//      // Get the end points as Point2D objects so we can use them in the
//      // builder
//      val pStart = layout.transform(endpoints.getFirst)
//      val pEnd = layout.transform(endpoints.getSecond)
//      // Draw the line
//      val line = LineBuilder.create.startX(pStart.getX).startY(pStart.getY).endX(pEnd.getX).endY(pEnd.getY).build
//      // add the edges to the screen
//      this.getChildren.add(line)
//    }
//  }
}

