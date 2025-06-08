package com.similaritysoftwares.getoverme.ui.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val orangeBalls = mutableListOf<Ball>()
    private val blueBalls = mutableListOf<Ball>()
    private val paint = Paint()
    private val borderPaint = Paint()
    private var lastUpdateTime = System.currentTimeMillis()
    private val random = Random(System.currentTimeMillis())

    private var heldBall: Ball? = null
    private var touchOffsetX: Float = 0f
    private var touchOffsetY: Float = 0f
    private var isGameOver = false
    private var isGameWon = false
    private var gameListener: GameListener? = null

    private var animationRunnable: Runnable? = null

    interface GameListener {
        fun onGameOver()
        fun onGameWon()
    }

    fun setGameListener(listener: GameListener) {
        gameListener = listener
    }

    init {
        borderPaint.color = Color.BLACK
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 8f // Thicker border

        // Create balls initially
        createInitialBalls()

        // Start the animation loop
        startAnimationLoop()
    }

    private fun createInitialBalls() {
        orangeBalls.clear()
        blueBalls.clear()
        isGameOver = false // Reset game state
        isGameWon = false
         // Create orange balls (even faster)
        repeat(7) { // Increased number of balls slightly
            orangeBalls.add(createBall(Color.rgb(255, 140, 0), 60f)) // Increased speed
        }
        // Create blue balls (even faster for interaction)
        repeat(7) { // Decreased number of blue balls
            blueBalls.add(createBall(Color.BLUE, 30f)) // Keeping user's last speed
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Re-create balls when size changes to ensure they are within bounds
        createInitialBalls()

        // Restart the game loop if needed
        if (!isGameOver && !isGameWon) {
             stopAnimationLoop()
             startAnimationLoop()
        }

    }

    private fun createBall(color: Int, speed: Float): Ball {
        val radius = 40f // Increased ball radius
        val maxAttempts = 100 // Prevent infinite loops if placement is difficult
        var attempts = 0
        while (attempts < maxAttempts) {
            val x = random.nextFloat() * (width - 2 * radius) + radius
            val y = random.nextFloat() * (height - 2 * radius) + radius
            var collision = false
            // Check for overlap with existing balls
            (orangeBalls + blueBalls).forEach { existingBall ->
                val dx = existingBall.x - x
                val dy = existingBall.y - y
                val distance = sqrt(dx * dx + dy * dy)
                if (distance < radius + existingBall.radius) {
                    collision = true
                    return@forEach // Exit the inner loop
                }
            }
            if (!collision) {
                // Found a valid position
                val angle = random.nextFloat() * 2 * Math.PI
                return Ball(
                    x = x,
                    y = y,
                    radius = radius,
                    speedX = (cos(angle) * speed).toFloat(),
                    speedY = (sin(angle) * speed).toFloat(),
                    color = color
                )
            }
            attempts++
        }
        // If unable to place without collision after attempts, place at a default spot (shouldn't happen often)
        val angle = random.nextFloat() * 2 * Math.PI
        return Ball(
            x = radius,
            y = radius,
            radius = radius,
            speedX = (cos(angle) * speed).toFloat(),
            speedY = (sin(angle) * speed).toFloat(),
            color = color
        )
    }

    private fun startAnimationLoop() {
        if (animationRunnable == null) {
            animationRunnable = object : Runnable {
                override fun run() {
                    if (!isGameOver && !isGameWon) {
                        // Update
                        val currentTime = System.currentTimeMillis()
                        val deltaTime = (currentTime - lastUpdateTime) / 1000f
                        lastUpdateTime = currentTime

                        // Update balls (only if not held)
                        val ballsToUpdate = (orangeBalls + blueBalls).filter { it != heldBall }
                        updateBalls(ballsToUpdate, deltaTime)

                        // Check collisions for held ball (only check if a ball is held)
                        heldBall?.let { ball ->
                            val removedBlueBalls = mutableListOf<Ball>()
                            // Check collision with other blue balls
                            blueBalls.filter { it != ball }.forEach { otherBlueBall ->
                                if (isColliding(ball, otherBlueBall)) {
                                    removedBlueBalls.add(otherBlueBall)
                                }
                            }
                            blueBalls.removeAll(removedBlueBalls)

                            // Check collision with orange balls
                            orangeBalls.forEach { orangeBall ->
                                if (isColliding(ball, orangeBall)) {
                                    triggerGameOver()
                                    return@run // Stop updating if game over
                                }
                            }

                            // Check for win condition after collecting blue balls
                            if (blueBalls.size == 1 && !isGameWon) { // Win when 1 blue ball remains
                                triggerGameWon()
                                return@run // Stop updating if game won
                            }
                        }

                         // Request a redraw after updates
                        postInvalidate()

                        // Schedule the next frame
                         postDelayed(this, 16) // ~60 FPS
                    }
                }
            }
            // Post the first frame
            postDelayed(animationRunnable!!, 16)
        }
    }

    private fun stopAnimationLoop() {
        animationRunnable?.let { removeCallbacks(it) }
        animationRunnable = null
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw border
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

        // Always draw balls (updates are handled in the animationRunnable)
        drawBalls(canvas, orangeBalls)
        drawBalls(canvas, blueBalls)
    }

    private fun isColliding(ball1: Ball, ball2: Ball): Boolean {
        val dx = ball2.x - ball1.x
        val dy = ball2.y - ball1.y
        val distance = sqrt(dx * dx + dy * dy)
        return distance < ball1.radius + ball2.radius
    }

    private fun updateBalls(balls: List<Ball>, deltaTime: Float) {
        balls.forEach { ball ->
            ball.x += ball.speedX * deltaTime
            ball.y += ball.speedY * deltaTime

            // Boundary collision
            if (ball.x - ball.radius < 0 || ball.x + ball.radius > width) {
                ball.speedX *= -1
                if (ball.color == Color.rgb(255, 140, 0)) {
                    normalizeSpeed(ball, 100f) // Maintain constant speed for orange balls
                }
                ball.x = if (ball.x - ball.radius < 0) ball.radius else (width - ball.radius)
            }
            if (ball.y - ball.radius < 0 || ball.y + ball.radius > height) {
                ball.speedY *= -1
                if (ball.color == Color.rgb(255, 140, 0)) {
                    normalizeSpeed(ball, 100f) // Maintain constant speed for orange balls
                }
                ball.y = if (ball.y - ball.radius < 0) ball.radius else (height - ball.radius)
            }
        }

        // Ball-to-ball collision
        for (i in balls.indices) {
            for (j in i + 1 until balls.size) {
                val ball1 = balls[i]
                val ball2 = balls[j]
                if (isColliding(ball1, ball2)) {
                    handleCollision(ball1, ball2)
                }
            }
        }
    }

    private fun handleCollision(b1: Ball, b2: Ball) {
        // Calculate velocities along the line of impact
        val dx = b2.x - b1.x
        val dy = b2.y - b1.y
        val distance = sqrt(dx * dx + dy * dy)

        // Normalize collision vector
        val nx = dx / distance
        val ny = dy / distance

        // Tangent vector
        val tx = -ny
        val ty = nx

        // Dot product of velocity and normal/tangent vectors
        val dpTan1 = b1.speedX * tx + b1.speedY * ty
        val dpNor1 = b1.speedX * nx + b1.speedY * ny
        val dpTan2 = b2.speedX * tx + b2.speedY * ty
        val dpNor2 = b2.speedX * nx + b2.speedY * ny

        // Conservation of momentum for normal component (assuming equal mass)
        val m1 = 1f // Assuming equal mass for simplicity
        val m2 = 1f
        val p = 2 * (dpNor1 - dpNor2) / (m1 + m2)

        // Update velocities
        b1.speedX = dpTan1 * tx + (dpNor1 - p) * nx
        b1.speedY = dpTan1 * ty + (dpNor1 - p) * ny
        b2.speedX = dpTan2 * tx + (dpNor2 + p) * nx
        b2.speedY = dpTan2 * ty + (dpNor2 + p) * ny

        // Correct position to prevent sticking
        val overlap = b1.radius + b2.radius - distance
        val correction = overlap / 2.0f

        b1.x -= correction * nx
        b1.y -= correction * ny
        b2.x += correction * nx
        b2.y += correction * ny

        // Normalize speed for orange balls after collision
        if (b1.color == Color.rgb(255, 140, 0)) {
            normalizeSpeed(b1, 100f)
        }
        if (b2.color == Color.rgb(255, 140, 0)) {
            normalizeSpeed(b2, 100f)
        }
    }

    private fun normalizeSpeed(ball: Ball, targetSpeed: Float) {
        val currentSpeed = sqrt(ball.speedX * ball.speedX + ball.speedY * ball.speedY)
        if (currentSpeed > 0) {
            ball.speedX = (ball.speedX / currentSpeed) * targetSpeed
            ball.speedY = (ball.speedY / currentSpeed) * targetSpeed
        }
    }

    private fun drawBalls(canvas: Canvas, balls: List<Ball>) {
        balls.forEach { ball ->
            paint.color = ball.color
            canvas.drawCircle(ball.x, ball.y, ball.radius, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isGameOver || isGameWon) return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if a blue ball is touched
                val touchX = event.x
                val touchY = event.y
                heldBall = blueBalls.find { ball ->
                    val dx = touchX - ball.x
                    val dy = touchY - ball.y
                    sqrt(dx * dx + dy * dy) < ball.radius
                }
                // If a blue ball is held, calculate offset
                heldBall?.let { ball ->
                    touchOffsetX = touchX - ball.x
                    touchOffsetY = touchY - ball.y
                    // Stop its movement while held
                    ball.speedX = 0f
                    ball.speedY = 0f
                    // Invalidate to redraw immediately
                    invalidate()
                    return true // Consume the event
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // Update position of held ball
                heldBall?.let { ball ->
                    val newX = event.x - touchOffsetX
                    val newY = event.y - touchOffsetY

                    // Clamp ball position within bounds
                    ball.x = newX.coerceIn(ball.radius, width - ball.radius)
                    ball.y = newY.coerceIn(ball.radius, height - ball.radius)

                    // Invalidate to redraw
                    invalidate()
                    return true // Consume the event
                }
            }
            MotionEvent.ACTION_UP -> {
                // Release the held ball
                heldBall?.let { ball ->
                    // Optional: Give it a small push in the direction of last movement
                     // val releaseSpeed = 5f // Example release speed
                     // ball.speedX = (event.getX(event.pointerCount - 1) - (event.getHistoricalX(event.pointerCount - 1, 0) + touchOffsetX)) / event.historySize * releaseSpeed
                     // ball.speedY = (event.getY(event.pointerCount - 1) - (event.getHistoricalY(event.pointerCount - 1, 0) + touchOffsetY)) / event.historySize * releaseSpeed

                    heldBall = null
                    invalidate()
                    return true // Consume the event
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun triggerGameOver() {
        isGameOver = true
        gameListener?.onGameOver()
        stopAnimationLoop() // Stop the animation loop on game over
        invalidate() // Draw the final state
    }

     private fun triggerGameWon() {
        isGameWon = true
        gameListener?.onGameWon()
        stopAnimationLoop() // Stop the animation loop on game won
        invalidate() // Draw the final state
    }

    // Data class for Ball remains the same
    private data class Ball(
        var x: Float,
        var y: Float,
        val radius: Float,
        var speedX: Float,
        var speedY: Float,
        val color: Int
    )

    // Ensure animation stops when view is detached
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimationLoop()
    }
} 