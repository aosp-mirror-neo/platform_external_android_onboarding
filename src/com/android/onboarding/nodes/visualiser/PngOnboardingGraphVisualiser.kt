package com.android.onboarding.nodes.visualiser

import com.android.onboarding.nodes.OnboardingGraph
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Polygon
import java.awt.Rectangle
import java.awt.geom.Line2D
import java.awt.image.BufferedImage
import java.lang.IllegalStateException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Visualiser which produces a PNG image representing an [OnboardingGraph]. */
object PngOnboardingGraphVisualiser {

  /** The number of seconds to pad to the left and right of the timeline. */
  private const val SECONDS_PADDING = 5L
  /** The number of pixels used per second of time on the timeline. */
  private const val PIXELS_PER_SECOND = 50
  /** The number of seconds between each vertical line on the timeline. */
  private const val SECONDS_PER_LINE = 1L
  /** The Y co-ordinate where we will start our first node. */
  private const val GRAPH_VERTICAL_PADDING = 70
  /** The padding beneath a node before we start the next level of embed. */
  private const val NODE_VERTICAL_PADDING = 50
  /** The height of each node. */
  private const val NODE_HEIGHT = 100

  /** An arrow which can be printed on the png. */
  private val arrowHead =
    Polygon().apply {
      addPoint(0, 5)
      addPoint(-5, -5)
      addPoint(5, -5)
    }

  /** A class used to track general data about the graph we are visualising. */
  data class GraphData(
    /** The earliest timestamp present on the graph. */
    val earliestTimestamp: Instant,
    /** The latest timestamp present on the graph. */
    val latestTimestamp: Instant,
    /** The maximum level any single node is embedded. A flat graph will have a value of 0. */
    val maxNodeDepth: Int,

    /** The width (in pixels) of the graph. */
    val width: Int,
    /** The height (in pixels) of the graph. */
    val height: Int
  ) {

    /** Gets the pixel which corresponds to a particular time on the timeline. */
    fun instantToPixel(instant: Instant) =
      (PIXELS_PER_SECOND * Duration.between(earliestTimestamp, instant).seconds).toInt()
  }

  /** Extracts graph data from an [OnboardingGraph]. */
  private fun extractGraphData(graph: OnboardingGraph): GraphData {
    var earliestTimestamp = Instant.MAX
    var latestTimestamp = Instant.MIN

    for (node in graph.nodes.values) {
      node.start.takeIf { it.isBefore(earliestTimestamp) }?.let { earliestTimestamp = it }
      node.end.takeIf { it.isAfter(latestTimestamp) }?.let { latestTimestamp = it }
    }

    // Then we apply padding
    earliestTimestamp =
      earliestTimestamp.minusSeconds(SECONDS_PADDING).truncatedTo(ChronoUnit.SECONDS)
    latestTimestamp = latestTimestamp.plusSeconds(SECONDS_PADDING).truncatedTo(ChronoUnit.SECONDS)

    val maxNodeDepth =
      graph.nodes.values.maxOfOrNull { it.embedLevel }
        ?: throw IllegalStateException("Cannot render graph with no nodes")

    val duration = Duration.between(earliestTimestamp, latestTimestamp)
    val width = (duration.seconds * PIXELS_PER_SECOND).toInt()
    val height =
      ((maxNodeDepth + 1) * (NODE_HEIGHT + NODE_VERTICAL_PADDING)) + (GRAPH_VERTICAL_PADDING * 2)

    return GraphData(earliestTimestamp, latestTimestamp, maxNodeDepth, width, height)
  }

  /** Returns a [BufferedImage] of a rendering of the [OnboardingGraph]. */
  fun visualise(graph: OnboardingGraph): BufferedImage {
    val graphData = extractGraphData(graph)

    val bufferedImage = BufferedImage(graphData.width, graphData.height, BufferedImage.TYPE_INT_RGB)
    val g2d = bufferedImage.createGraphics()

    g2d.setColor(Color.white)
    g2d.fillRect(0, 0, graphData.width, graphData.height)

    drawTimeline(g2d, graphData)
    drawNodes(g2d, graphData, graph)

    g2d.dispose()

    return bufferedImage
  }

  /**
   * Draw the timeline for the graph.
   *
   * This is the timestamp labels and the ticks.
   */
  fun drawTimeline(g2d: Graphics2D, graphData: GraphData) {
    val formatter = DateTimeFormatter.ofPattern("H:m:s").withZone(ZoneId.systemDefault())
    val font = Font(g2d.font.toString(), 0, 12)

    g2d.setColor(Color.gray)
    g2d.fillRect(0, 40, graphData.width, 4)
    var time = graphData.earliestTimestamp
    while (time <= graphData.latestTimestamp) {
      val pixelsFromLeft = graphData.instantToPixel(time)

      g2d.fillRect(pixelsFromLeft, 40, 1, 600)

      drawCenteredString(
        g2d,
        formatter.format(time),
        Rectangle(pixelsFromLeft - (PIXELS_PER_SECOND), 20, PIXELS_PER_SECOND * 2, 20),
        font
      )

      time = time.plusSeconds(SECONDS_PER_LINE)
    }
  }

  /** Draw nodes in the graph and connections between them. */
  private fun drawNodes(g2d: Graphics2D, graphData: GraphData, graph: OnboardingGraph) {
    val font = Font(g2d.font.toString(), 0, 12)

    for (node in graph.nodes.values) {
      val embed = node.embedLevel

      // We offset the start by half a second to make transitions visible
      var startXpixel = graphData.instantToPixel(node.start) + (PIXELS_PER_SECOND / 2)
      var endXpixel = graphData.instantToPixel(node.end)
      var startYpixel = GRAPH_VERTICAL_PADDING + embed * (NODE_HEIGHT + NODE_VERTICAL_PADDING)

      g2d.setColor(Color.white)
      g2d.fillRect(startXpixel, startYpixel, endXpixel - startXpixel, NODE_HEIGHT)

      g2d.setColor(Color.black)
      g2d.drawRect(startXpixel, startYpixel, endXpixel - startXpixel, NODE_HEIGHT)

      drawCenteredString(
        g2d,
        node.name,
        Rectangle(startXpixel, startYpixel, endXpixel - startXpixel, NODE_HEIGHT),
        font
      )

      // If there is an incoming edge - draw it
      node.incomingEdge?.let {
        if (it.node.end <= node.start) {
          // The incoming node ended when starting this one - so no need to embed further

          val transitionXStart = graphData.instantToPixel(it.timestamp).toDouble()
          val transitionXEnd =
            graphData.instantToPixel(it.timestamp).toDouble() + (PIXELS_PER_SECOND / 2)

          // Draw a right-facing arrow between the nodes
          drawArrowLine(
            g2d,
            Line2D.Double(
              transitionXStart,
              (GRAPH_VERTICAL_PADDING.toDouble() +
                (embed) * (NODE_HEIGHT + NODE_VERTICAL_PADDING)) + (NODE_HEIGHT / 2),
              transitionXEnd - 5, // Slightly to the left of the start to leave room for the arrow
              (GRAPH_VERTICAL_PADDING.toDouble() +
                (embed) * (NODE_HEIGHT + NODE_VERTICAL_PADDING)) + (NODE_HEIGHT / 2)
            )
          )
        } else {
          // The incoming node is still running - so we need to draw this node below the other one

          val transitionX =
            graphData.instantToPixel(it.timestamp).toDouble() + (PIXELS_PER_SECOND / 2)
          val returnTimeX = graphData.instantToPixel(node.end).toDouble()

          // Draw line showing the node start
          drawArrowLine(
            g2d,
            Line2D.Double(
              transitionX,
              (GRAPH_VERTICAL_PADDING.toDouble() +
                (embed - 1) * (NODE_HEIGHT + NODE_VERTICAL_PADDING)) + NODE_HEIGHT,
              transitionX,
              (startYpixel - 5)
                .toDouble() // Slightly above the start of this node to leave room for the arrow
            )
          )

          // Draw line showing the return
          drawArrowLine(
            g2d,
            Line2D.Double(
              returnTimeX,
              startYpixel.toDouble(),
              returnTimeX,
              (GRAPH_VERTICAL_PADDING.toDouble() +
                (embed - 1) * (NODE_HEIGHT + NODE_VERTICAL_PADDING)) +
                NODE_HEIGHT +
                5 // Slightly below the end of the previous node to leave room for the arrow
            )
          )
        }
      }
    }
  }

  /** Draw a line with an arrow on the end. */
  private fun drawArrowLine(g2d: Graphics2D, line: Line2D.Double) {
    g2d.drawLine(line.x1.toInt(), line.y1.toInt(), line.x2.toInt(), line.y2.toInt())
    var tx = g2d.getTransform()
    val angle = Math.atan2(line.y2 - line.y1, line.x2 - line.x1)
    tx.translate(line.x2, line.y2)
    tx.rotate(angle - Math.PI / 2.0)
    val g: Graphics2D = g2d.create() as Graphics2D
    g.setTransform(tx)
    g.fill(arrowHead)
    g.dispose()
  }

  /** Draw a string centered in the provided [rect]. */
  private fun drawCenteredString(g: Graphics2D, text: String, rect: Rectangle, font: Font) {
    val metrics = g.getFontMetrics(font)
    val stringWidth = metrics.stringWidth(text)
    val stringHeight = metrics.getHeight()

    val x: Int = rect.x + (rect.width - stringWidth) / 2
    val y: Int = rect.y + (rect.height - stringHeight) / 2

    g.setFont(font)
    g.drawString(text, x, y)
  }

  @JvmStatic
  fun main(args: Array<String>) {
  }
}

val OnboardingGraph.Node.embedLevel: Int
  get() {
    var embed = 0

    var n: OnboardingGraph.Node? = this
    while (n != null) {
      if (n.incomingEdge?.node?.end?.let { it <= n!!.start } != false) {
        return embed
      }

      n = n.incomingEdge?.node ?: return embed

      embed += 1
    }

    return embed
  }
