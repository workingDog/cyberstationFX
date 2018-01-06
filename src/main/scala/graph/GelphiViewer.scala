//package graph
//
//import java.io.File
//import java.io.FileNotFoundException
//import java.net.URISyntaxException
//import com.sun.javafx.geom.Edge
//import org.gephi.data.attributes.api.AttributeController
//import org.gephi.data.attributes.api.AttributeModel
//import org.gephi.filters.api.FilterController
//import org.gephi.graph.api.DirectedGraph
//import org.gephi.graph.api.Edge
//import org.gephi.graph.api.GraphController
//import org.gephi.graph.api.GraphModel
//import org.gephi.graph.api.Node
//import org.gephi.io.importer.api.Container
//import org.gephi.io.importer.api.EdgeDefault
//import org.gephi.io.importer.api.ImportController
//import org.gephi.io.processor.plugin.DefaultProcessor
//import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2
//import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2LayoutData
//import org.gephi.preview.api.PreviewController
//import org.gephi.preview.api.PreviewModel
//import org.gephi.project.api.ProjectController
//import org.gephi.project.api.Workspace
//import org.gephi.ranking.api.RankingController
//import org.joda.time.field.OffsetDateTimeField
//import org.openide.util.Lookup
//import com.itextpdf.text.Font
//import processing.core.PApplet
//import processing.core.PFont
//import processing.core.PVector
//
//
//// see https://gist.github.com/MoritzStefaner/3423911
//
//class GelphiViewer {
//  private var graphModel = null
//  private var layout = null
//  private var maxY = -10000
//  private var minX = 10000
//  private var minY = 10000
//  private var maxX = -10000
//  private var regularFont = null
//
//  def setup(): Unit = {
//    size(1000, 1000, OPENGL)
//    regularFont = loadFont("HelveticaNeue-Medium-48.vlw")
//    //Init a project - and therefore a workspace
//    val pc = Lookup.getDefault.lookup(classOf[Nothing])
//    pc.newProject
//    val workspace = pc.getCurrentWorkspace
//    //Get models and controllers for this new workspace - will be useful later
//    val attributeModel = Lookup.getDefault.lookup(classOf[Nothing]).getModel
//    graphModel = Lookup.getDefault.lookup(classOf[Nothing]).getModel
//    val model = Lookup.getDefault.lookup(classOf[Nothing]).getModel
//    val importController = Lookup.getDefault.lookup(classOf[Nothing])
//    val filterController = Lookup.getDefault.lookup(classOf[Nothing])
//    val rankingController = Lookup.getDefault.lookup(classOf[Nothing])
//    //Import file
//    var container = null
//    var file = null
//    try
//      file = new File(getClass.getResource("/<your gephi file>.gexf").toURI)
//    catch {
//      case e: URISyntaxException =>
//        // TODO Auto-generated catch block
//        e.printStackTrace()
//    }
//    try
//      container = importController.importFile(file)
//    catch {
//      case e: FileNotFoundException =>
//        e.printStackTrace()
//    }
//    container.getLoader.setEdgeDefault(EdgeDefault.DIRECTED) //Force DIRECTED
//
//    //Append imported data to GraphAPI
//    importController.process(container, new Nothing, workspace)
//    //See if graph is well imported
//    val graph = graphModel.getDirectedGraph
//    System.out.println("Nodes: " + graph.getNodeCount)
//    System.out.println("Edges: " + graph.getEdgeCount)
//    layout = new Nothing(null)
//    layout.setGraphModel(graphModel)
//    layout.resetPropertiesValues
//    layout.setOutboundAttractionDistribution(false)
//    layout.asInstanceOf[Nothing].setEdgeWeightInfluence(1.5d)
//    layout.asInstanceOf[Nothing].setGravity(10d)
//    layout.asInstanceOf[Nothing].setJitterTolerance(.02)
//    layout.asInstanceOf[Nothing].setScalingRatio(15.0)
//    layout.initAlgo
//  }
//
//  def draw(): Unit = {
//    background(255)
//    textFont(regularFont)
//    noStroke
//    minX = 10000
//    minY = 10000
//    maxX = -10000
//    maxY = -10000
//    var x1 = .0
//    var x2 = .0
//    var y1 = .0
//    var y2 = .0
//    var w = .0
//    import scala.collection.JavaConversions._
//    for (n <- graphModel.getDirectedGraph.getNodes) {
//      minX = Math.min(minX, n.getNodeData.x)
//      maxX = Math.max(maxX, n.getNodeData.x)
//      minY = Math.min(minY, n.getNodeData.y)
//      maxY = Math.max(maxY, n.getNodeData.y)
//    }
//    import scala.collection.JavaConversions._
//    for (n <- graphModel.getDirectedGraph.getNodes) {
//      x1 = x(n.getNodeData.x)
//      y1 = y(n.getNodeData.y)
//      w = Math.sqrt(1.0f * n.getNodeData.getAttributes.getValue("Weighted In-Degree").asInstanceOf[Integer]).toFloat
//      fill(30, w * 10 + 90)
//      ellipse(x1, y1, 5, 5)
//      textSize((1.5f * w + 3).toInt)
//      val l = n.getNodeData.getAttributes.getValue("label").toString
//      val offset = map(x1 - width * .5f, width * .5f, -width * .5f, 0f, textWidth(l))
//      text(l, x1 - offset, y1 - 2)
//      n.getNodeData.getLayoutData.asInstanceOf[Nothing].mass = 1 + w * 5
//    }
//    layout.goAlgo
//    val normal = new Nothing
//    import scala.collection.JavaConversions._
//    for (e <- graphModel.getDirectedGraph.getEdges) {
//      x1 = x(e.getEdgeData.getSource.x)
//      x2 = x(e.getEdgeData.getTarget.x)
//      y1 = y(e.getEdgeData.getSource.y)
//      y2 = y(e.getEdgeData.getTarget.y)
//      normal.set(y2 - y1, -(x2 - x1), 0f)
//      normal.normalize
//      w = e.getEdgeData.getAttributes.getValue("weight").asInstanceOf[Float]
//      beginShape
//      fill(90, 80, 70, w * 30)
//      vertex(x2 + normal.x * w, y2 + normal.y * w)
//      vertex(x1, y1)
//      vertex(x2 - normal.x * w, y2 - normal.y * w)
//      endShape(CLOSE)
//    }
//  }
//
//  private def x(x: Float) = map(x, minX, maxX, 50, width - 100)
//
//  private def y(y: Float) = map(y, minY, maxY, 50, height - 100)
//}
//
//
