import org.openrndr.ExtensionStage
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.color.presets.DARK_GREY
import org.openrndr.extra.color.presets.LIGHT_BLUE
import org.openrndr.extra.color.presets.LIGHT_CORAL
import org.openrndr.extra.color.presets.LIGHT_GREEN
import org.openrndr.extra.color.presets.WHITE_SMOKE
import org.openrndr.panel.elements.round
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.GUIAppearance
import org.openrndr.extra.parameters.*

fun main() = application {
    configure {
        width = 1000
        height = 500
        minimumWidth = 1000
        minimumHeight = 500
        windowResizable = true
        title = "Simulatore Balistico ~ by MattiaDev"
    }

    program {
        val gui = GUI(
            GUIAppearance(
                baseColor = ColorRGBa.WHITE_SMOKE.opacify(0.5),
                barWidth = 250
            )
        )

        val settings = object {
            @DoubleParameter("Initial velocity", 0.0, 30.0)
            var v0 = 10.0

            @DoubleParameter("Angle", -90.0, 90.0)
            var angle = 45.0

            @DoubleParameter("Initial height", 0.0, 15.0)
            var initialHeight = 5.0

            @BooleanParameter("Start")
            var start = false

        }

        gui.add(settings, "Projectile settings")
        extend(gui)

        val camera = Camera2D()
        extend(camera)


        backgroundColor = ColorRGBa.LIGHT_BLUE

        val hInM = 20
        val wInM = 25
        val hToPx = height.toDouble()/hInM
        val wToPx = width.toDouble()/wInM

        val scale = min(hToPx, wToPx)

        val g: Double = 9.81 // m/s^2

        var h: Double = if(settings.initialHeight * scale > 5.0) height - 50.0 - (settings.initialHeight * scale)
        else height - 50.0 - 5.0 - (settings.initialHeight * scale)
        var angle: Double = Math.toRadians(settings.angle)

        var y: Double = h
        var x: Double = 0.0

        var v0: Double = settings.v0 // initial velocity in m/s
        var vx0: Double = v0 * cos(angle)
        var vy0: Double = v0 * sin(angle)

        var G: Double = 0.0
        var tVolo: Float = 0f
        var yMax: Double = height.toDouble()
        var hMax: Double = 0.0

        var t: Double = 0.0
        var t0: Double = 0.0
        var last: Double = 0.0
        var landed = false

        fun drawScene() {
            drawer.fill = ColorRGBa.LIGHT_GREEN
            drawer.rectangle(-(x + width.toDouble()), height.toDouble() - 50.0, 2 * (x + width.toDouble()) * 10, 50.0)

            for(i in 0..(hInM-2)*2){
                drawer.fill = if(i%2 == 0) ColorRGBa.LIGHT_CORAL
                else ColorRGBa.WHITE_SMOKE

                drawer.rectangle(30.0, height-50.0-0.5*scale-(i*0.5*scale), 40.0, 0.5*scale)
            }

            val maxDistance = (x + width.toDouble() / scale).toInt()

            for (i in 0..maxDistance / 10) {
                val distance = i * 10.0
                val markerX = 75.0 + distance * scale

                drawer.fill = if (i % 2 == 0)
                    ColorRGBa.WHITE
                else
                    ColorRGBa.LIGHT_CORAL

                drawer.rectangle(
                    markerX - 3.0,
                    height - 50.0,
                    6.0,
                    12.0
                )

                drawer.fill = ColorRGBa.WHITE
                drawer.rectangle(
                    markerX - 12.0,
                    height - 62.0,
                    24.0,
                    14.0
                )

                drawer.fill = ColorRGBa.BLACK
                drawer.text(
                    "${distance.toInt()}",
                    markerX - 8.0,
                    height - 52.0
                )
            }
        }

        extend {
            if(settings.start) {
                if (!landed) {
                    val dt = seconds - last
                    last = seconds

                    t = seconds - t0

                    y = h - (vy0 * t - 0.5 * g * t * t) * scale
                    x = vx0 * t * scale

                    yMax = if (y < yMax) y
                    else yMax

                    hMax = if(yMax > 0) height - yMax - 50.0
                    else height - 50 - yMax

                    var s: Double = t - dt
                    while(s <= t){ // to increase sampling rate i interpolate the frames in between one frame and the next.
                        val interY = h - (vy0 * s - 0.5 * g * s * s) * scale
                        val interX = vx0 * s * scale
                        if (interY+5 >= height-50.0) {
                            landed = true
                            tVolo = s.toFloat()
                            G = interX
                        }
                        s+=0.01
                    }
                } else {
                    x = G
                    y = height - 50 - 5.0
                }

                drawer.fill = ColorRGBa.WHITE_SMOKE
                drawer.circle(x=75.0+x, y=y, 10.0)

            } else {
                last = seconds
                t0 = seconds

                h = if(settings.initialHeight * scale > 5.0) height - 50.0 - (settings.initialHeight * scale)
                else height - 50.0 - 5.0 - (settings.initialHeight * scale)
                angle = Math.toRadians(settings.angle)

                v0 = settings.v0
                vx0 = v0 * cos(angle)
                vy0 = v0 * sin(angle)

                x = 75.0
                y = h
                t = 0.0
                yMax = height.toDouble()
                landed = false


                drawer.fill = ColorRGBa.WHITE_SMOKE
                drawer.circle(x, y, 10.0)
                drawer.strokeWeight = 2.5
                drawer.lineSegment(x, y, x+30*cos(angle), y-30*sin(angle))
                drawer.strokeWeight = 0.5
            }
            drawScene()
        }

        extend(ExtensionStage.AFTER_DRAW) {
            if (landed) {
                drawer.fill = ColorRGBa.BLACK
                drawer.text("T_volo = " + tVolo.toString(), x + 10, y - 30.0)

                drawer.fill = ColorRGBa.BLACK
                drawer.text("Gittata = " + ((G-5.0) / scale).round(2).toString(), x + 10, y - 50.0)

                drawer.fill = ColorRGBa.BLACK
                drawer.text("Y MAX = " + (hMax / scale).round(2).toString(), x + 10, y - 70.0)
            } else {
                drawer.fill = ColorRGBa.WHITE
                drawer.text(t.round(2).toString(), width - 60.0, height - 30.0)
            }
        }
    }
}
