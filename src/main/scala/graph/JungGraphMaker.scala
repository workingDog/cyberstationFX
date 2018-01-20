//package graph
//
//import java.awt.geom.{AffineTransform, Ellipse2D, Point2D, Rectangle2D}
//
//import edu.uci.ics.jung.algorithms.layout._
//import edu.uci.ics.jung.visualization.control.{DefaultModalGraphMouse, ModalGraphMouse}
//import com.kodekutters.stix._
//import edu.uci.ics.jung.graph.{DirectedSparseGraph, Graph}
//
//import scalafx.scene.layout.Pane
//import edu.uci.ics.jung.visualization.VisualizationViewer
//import java.awt.{Color, Dimension, Rectangle, RenderingHints}
//import java.util.{Timer, TimerTask}
//
//import com.typesafe.config.{Config, ConfigFactory}
//
//import scala.collection.mutable
//import scalafx.embed.swing.SwingNode
//import edu.uci.ics.jung.visualization.decorators.{EdgeShape, PickableVertexPaintTransformer, ToStringLabeller}
//import edu.uci.ics.jung.visualization.renderers.CenterEdgeArrowRenderingSupport
//import edu.uci.ics.jung.algorithms.layout.util.VisRunner
//import edu.uci.ics.jung.algorithms.util.IterativeContext
//import edu.uci.ics.jung.visualization.layout.LayoutTransition
//import edu.uci.ics.jung.visualization.util.Animator
//import edu.uci.ics.jung.visualization.renderers._
//
//
//
//case class JungGraphMaker(thePane: Pane) {
//
//  val config: Config = ConfigFactory.load
//  private var delay = 20000
//  try {
//    delay = config.getInt("animation.delay")
//  } catch {
//    case e: Throwable => println("---> config animation.delay error: " + e)
//  }
//
//  val viewSize = new Dimension(thePane.width.value.toInt, thePane.height.value.toInt)
//  val pList: mutable.HashMap[StixNode, Point2D] = mutable.HashMap()
//  val graph: Graph[StixNode, StixEdge] = new DirectedSparseGraph[StixNode, StixEdge]
//  val layout = new SpringLayout[StixNode, StixEdge](graph)
//  val graphMouse = new DefaultModalGraphMouse[StixNode, StixEdge]
//  //  graphMouse.setMode (ModalGraphMouse.Mode.TRANSFORMING)
//  graphMouse.setMode(ModalGraphMouse.Mode.PICKING)
//
//  val viewer = new VisualizationViewer[StixNode, StixEdge](layout, viewSize)
//  viewer.setBackground(Color.white)
//  viewer.setGraphMouse(graphMouse)
//  viewer.getRenderContext.setVertexLabelTransformer((x) => x.toString)
//  viewer.getRenderContext.setEdgeLabelTransformer((x) => x.toString)
//  viewer.getRenderer.getVertexLabelRenderer.setPosition(Renderer.VertexLabel.Position.W)
//  // viewer.getRenderContext.setVertexShapeTransformer((x) => new Rectangle(-15, -10, 30, 20))
//  viewer.getRenderContext.setVertexShapeTransformer((x) => new Ellipse2D.Double(-15, -15, 30, 30))
//  viewer.getRenderContext.setVertexFillPaintTransformer(
//    new PickableVertexPaintTransformer[StixNode](viewer.getPickedVertexState, Color.pink, Color.red))
//  viewer.setVertexToolTipTransformer((x) => x.toString)
//  viewer.getRenderer.getEdgeRenderer.setEdgeArrowRenderingSupport(new CenterEdgeArrowRenderingSupport)
//
//  // to improve rendering speed ?
//  viewer.getRenderingHints().remove(RenderingHints.KEY_ANTIALIASING)
//
//  thePane.width.onChange { (_, _, newVal) =>
//    if (newVal != null) {
//      viewer.setSize(newVal.intValue(), viewer.getHeight)
//      //  viewer.setPreferredSize(new Dimension(newVal.intValue(), viewer.getHeight))
//      viewer.repaint()
//    }
//  }
//  thePane.height.onChange { (_, _, newVal) =>
//    if (newVal != null) {
//      viewer.setSize(viewer.getWidth, newVal.intValue())
//      //  viewer.setPreferredSize(new Dimension(viewer.getWidth, newVal.intValue()))
//      viewer.repaint()
//    }
//  }
//
//  val swingNode = new SwingNode()
//  swingNode.setContent(viewer)
//
//  val relaxer = new VisRunner(layout.asInstanceOf[IterativeContext])
//  val lt = new LayoutTransition[StixNode, StixEdge](viewer, viewer.getGraphLayout, layout)
//  val animator = new Animator(lt)
//
//  val timer = new Timer()
//
//  def stopAnimation() = {
//    relaxer.stop()
//    animator.stop()
//    layout.lock(true)
//  }
//
//  private def doForDelay() = {
//    timer.schedule(new TimerTask {
//      override def run(): Unit = {
//        stopAnimation()
//      }
//    }, delay)
//  }
//
//  def render(stixList: List[StixObj]): Unit = {
//    thePane.getChildren.clear()
//    makeGraph(stixList)
//    thePane.getChildren.add(swingNode)
//    animate()
//  }
//
//  private def animate() = {
//    layout.lock(false)
//    relaxer.stop()
//    relaxer.prerelax()
//    relaxer.relax()
//    animator.start()
//    viewer.repaint()
//    doForDelay()
//  }
//
//  private def makeGraph(stixList: List[StixObj]) = {
//    // process the nodes first
//    val nodeList = mutable.ListBuffer[StixNode]()
//    stixList.filter(_.isInstanceOf[SDO]).foreach(x => {
//      val n = StixNode(x.id.toString(), x.`type`) //x.asInstanceOf[SDO])
//      graph.addVertex(n)
//      nodeList += n
//    })
//    // then make the relations
//    stixList.filter(_.isInstanceOf[SRO]).foreach {
//      case x: Relationship =>
//        val source = nodeList.find(_.id == x.asInstanceOf[Relationship].source_ref.toString())
//        val target = nodeList.find(_.id == x.asInstanceOf[Relationship].target_ref.toString())
//        if (source.isDefined && target.isDefined) {
//          graph.addEdge(StixEdge(x.id.toString(), x.relationship_type), source.get, target.get)
//        }
//      case x: Sighting =>
//        val source = nodeList.find(_.id == x.asInstanceOf[Sighting].sighting_of_ref.toString())
//        if (source.isDefined) {
//          graph.addEdge(StixEdge(x.id.toString(), x.`type`), source.get, source.get)
//        }
//    }
//    layout.initialize()
//  }
//
//}
