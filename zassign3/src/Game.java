
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import javax.swing.*;

import javax.swing.ImageIcon;

//https://codeshack.io/images-sprite-sheet-generator/

import game2D.*;

// Game demonstrates how we can override the GameCore class
// to create our own 'game'. We usually need to implement at
// least 'draw' and 'update' (not including any local event handling)
// to begin the process. You should also add code to the 'init'
// method that will initialise event handlers etc. By default GameCore
// will handle the 'Escape' key to quit the game but you should
// override this with your own event handler.

/**
 * @author David Cairns
 *
 */
@SuppressWarnings("serial")

public class Game extends GameCore {
	// Useful game constants
	static int screenWidth = 1000;
	static int screenHeight = 648;

	float lift = 0.005f;
	float gravity = 0.0007f;

	int jumpCount = 0;
	long score; // The score will be the total time elapsed since a crash
	int levelNo = 1;
	int healthCount = 2;
	int heartXCoord = 4;

	// sounds
	Sound backgroundMusic;
	Sound jumpSound;
	Sound walkingSound;

	// state flags
	boolean up;
	boolean falling;
	boolean left;
	boolean right;
	boolean paused = false;
	boolean won = false;
	boolean hit = false;
	boolean dead = false;

	// recent direction string
	String recentAction = "";
	// Animations
	Animation idle;
	Animation idleLeft;
	Animation run;
	Animation runLeft;
	Animation jump;
	Animation jumpLeft;
	Animation ca;
	Animation portalAnim;
	Animation enemyRight;
	Animation enemyLeft;

	Sprite player = null;
	Sprite spawnPortal = null;
	Sprite endPortal = null;
	Sprite enemy = null;

	ArrayList<Sprite> clouds = new ArrayList<Sprite>();

	TileMap tmap = new TileMap(); // Our tile map, note that we load it in
	TileMap coinsCollected = new TileMap();								// init()
	TileMap health = new TileMap();
	
	/**
	 * The obligatory main method that creates an instance of our class and
	 * starts it running
	 * 
	 * @param args
	 *            The list of parameters this program might use (ignored)
	 */
	public static void main(String[] args) {

		Game gct = new Game();
		gct.init("level1.txt");
		// Start in windowed mode with the given screen height and width
		gct.run(false, screenWidth, screenHeight);
	}

	/**
	 * Initialise the class, e.g. set up variables, load images, create
	 * animations, register event handlers
	 */
	public void init(String level) {
		Sprite s; // Temporary reference to a sprite

		backgroundMusic = new Sound("sounds/Knockout.mid");
		backgroundMusic.start();

		// Load the tile map and print it out so we can check it is valid
		tmap.loadMap("maps", level);
		coinsCollected.loadMap("maps", "coinsCollected.txt");
		health.loadMap("maps", "health.txt");

		// Create a set of background sprites that we can
		// rearrange to give the illusion of motion

		idle = new Animation();
		idle.loadAnimationFromSheet("images/idleR.png", 10, 1, 60);

		idleLeft = new Animation();
		idleLeft.loadAnimationFromSheet("images/idleL.png", 10, 1, 60);

		run = new Animation();
		run.loadAnimationFromSheet("images/runR.png", 10, 1, 60);

		runLeft = new Animation();
		runLeft.loadAnimationFromSheet("images/runL.png", 10, 1, 60);

		jump = new Animation();
		jump.loadAnimationFromSheet("images/jumpR.png", 10, 1, 60);

		jumpLeft = new Animation();
		jumpLeft.loadAnimationFromSheet("images/jumpL.png", 10, 1, 60);

		ca = new Animation();
		ca.addFrame(loadImage("images/cloud.png"), 1000);

		portalAnim = new Animation();
		portalAnim.loadAnimationFromSheet("images/portal.png", 4, 1, 120);

		//enemyLeft = new Animation();
		//enemyLeft.loadAnimationFromSheet("images/enemyLeft.png", 10, 1, 60);

		enemyRight = new Animation();
		enemyRight.loadAnimationFromSheet("images/glooRotated.png", 2, 1, 60);

		// Initialise the player with an animation
		player = new Sprite(idle);
		player.setAnimationSpeed(1f);

		endPortal = new Sprite(portalAnim);
		endPortal.setAnimationSpeed(1f);
		
		spawnPortal = new Sprite(portalAnim);
		spawnPortal.setAnimationSpeed(1f);

		enemy = new Sprite(enemyRight);
		enemy.setAnimationSpeed(0.5f);
		// Create 3 clouds at random positions off the screen
		// to the right
		for (int c = 0; c < 3; c++) {
			s = new Sprite(ca);
			s.setX(screenWidth + (int) (Math.random() * 200.0f));
			s.setY(30 + (int) (Math.random() * 150.0f));
			s.setVelocityX(-0.02f);
			s.show();
			clouds.add(s);
		}

		initialiseGame();
		System.out.println(tmap);
		System.out.println(coinsCollected);
		System.out.println(health);
	}

	/**
	 * You will probably want to put code to restart a game in a separate method
	 * so that you can call it to restart the game.
	 */
	public void initialiseGame() {
		score = 0;

		player.setX(153);
		player.setY(120);
		player.setVelocityX(0);
		player.setVelocityY(0);
		player.show();
		
		spawnPortal.setX(150);
		spawnPortal.setY(120);
		spawnPortal.setVelocityX(0);
		spawnPortal.setVelocityY(0);
		spawnPortal.show();

		endPortal.setX(2000);
		endPortal.setY(433);
		endPortal.setVelocityX(0);
		endPortal.setVelocityY(0);
		endPortal.show();

		enemy.setX(500);
		enemy.setY(400);
		enemy.setVelocityX(0.1f);
		enemy.setVelocityY(0);
		enemy.show();
	}

	/**
	 * Draw the current state of the game
	 */
	public void draw(Graphics2D g) {
		// Be careful about the order in which you draw objects - you
		// should draw the background first, then work your way 'forward'
		// First work out how much we need to shift the view
		// in order to see where the player is.

		g.setColor(Color.cyan);
		g.fillRect(0, 0, getWidth(), getHeight());

		int xo = (int) -player.getX() + 150;
		int yo = 0;

		for (Sprite s : clouds) {
			s.setOffsets(xo, yo);
			s.draw(g);
		}

		enemy.setOffsets(xo, yo);
		spawnPortal.setOffsets(xo, yo);
		endPortal.setOffsets(xo, yo);
		player.setOffsets(xo, yo);
		
		enemy.draw(g);
		spawnPortal.draw(g);
		endPortal.draw(g);
		player.draw(g);
		tmap.draw(g, xo, yo);
		health.draw(g, 0, 0);
		coinsCollected.draw(g, 0, 0);

		String msg = String.format("Score: %d", score);
		g.setColor(Color.darkGray);
		g.drawString(msg, getWidth() - 80, 50);
		
		g.setColor(Color.darkGray);
		g.drawString("JUMP TO ENTER",1980 ,400);

		if (paused){
			g.setColor(Color.white);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.black);
			g.drawString("GAME PAUSED", (screenWidth / 2) - 15, screenHeight / 2);
			
		}
		
		if (dead) {
			paused = true;
			g.setColor(Color.white);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.black);
			g.drawString("YOU DIED", (screenWidth / 2) - 15, screenHeight / 2);
		}

		if (won) {
			paused = true;
			g.setColor(Color.white);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.black);
			g.drawString("YOU WON", (screenWidth / 2) - 15, screenHeight / 2);
		}
	}

	int iterations = 0;

	/**
	 * Update any sprites and check for collisions
	 * 
	 * @param elapsed
	 *            The elapsed time between this call and the previous call of
	 *            elapsed
	 */
	public void update(long elapsed) {
		if (paused)
			return;
		
		playerInput(elapsed);
		enemy.setVelocityY(enemy.getVelocityY() + (gravity * elapsed));
		player.setVelocityY(player.getVelocityY() + (gravity * elapsed));

		for (Sprite s : clouds)
			s.update(elapsed);

		// Now update the sprites animation and position
		enemy.update(elapsed);
		player.update(elapsed);
		spawnPortal.update(elapsed);
		endPortal.update(elapsed);

		// Then check for any collisions that may have occurred
		handleTileMapCollisions(player, elapsed);
		handleTileMapCollisions(enemy, elapsed);

		if (boundingBoxCollision(player, endPortal)) {
			if (levelNo == 1) {
				score = score + 100;
				switchMap();
				levelNo = 2;
			} else if (levelNo == 2) {

				won = true;
			}
		}

		if(!hit)
		if (boundingBoxCollision(player, enemy)) {
			if (iterations < 2) {
				health.setTileChar('.', heartXCoord, 1);
				heartXCoord--;
			} else if (iterations > 60) {
				iterations = 0;
			}
			iterations++;
		}
	}

	public void playerInput(long elapsed) {
		// if(falling){
		// player.setVelocityY(player.getVelocityY() + (gravity * elapsed));
		// }
		// Make adjustments to the speed of the sprite due to gravity
		// player.setVelocityY(player.getVelocityY()+(gravity*elapsed));

		if (up) {
			jumpSound = new Sound("sounds/jumpSound.wav");
			jumpSound.start();
			if (player.getVelocityY() >= 0 && jumpCount < 2) {
				if (player.getVelocityX() >= 0) {
					recentAction = "JumpRight";
					player.setAnimation(jump);
					player.setAnimationSpeed(0.3f);
					player.setVelocityY(-0.3f);
					player.shiftY(0.01f);
				}
				if (player.getVelocityX() <= 0) {
					recentAction = "JumpLeft";
					player.setAnimation(jumpLeft);
					player.setAnimationSpeed(0.3f);
					player.setVelocityY(-0.3f);
					player.shiftY(0.01f);
				}
				jumpCount++;
			} else if (jumpCount > 1 && player.getVelocityY() == 0) {
				jumpCount = 0;
			}

		}
		if (left) {
			recentAction = "RunLeft";
			player.setAnimation(runLeft);
			player.setAnimationSpeed(1.2f);
			player.setVelocityX(-0.2f);
		}
		if (right) {
			recentAction = "RunRight";
			player.setAnimation(run);
			player.setAnimationSpeed(1.2f);
			player.setVelocityX(0.2f);
		}
		if (!(right || left || up)) {
			if (recentAction.equals("RunRight") || recentAction.equals("JumpRight")) {
				player.setAnimation(idle);
			}
			if (recentAction.equals("RunLeft") || recentAction.equals("JumpLeft")) {
				player.setAnimation(idleLeft);
			}
			player.setAnimationSpeed(1.2f);
			player.setVelocityX(0f);
		}
	}

	/**
	 * Checks and handles collisions with the tile map for the given sprite 's'.
	 * Initial functionality is limited...
	 * 
	 * @param s
	 *            The Sprite to check collisions for
	 * @param elapsed
	 *            How time has gone by
	 */
	// public void handleTileMapCollisions(Sprite s, long elapsed) //cr
	// {
	// // This method should check actual tile map collisions. For
	// // now it just checks if the player has gone off the bottom
	// // of the tile map.
	//
	// int tileCoordX = (int) (s.getX() / tmap.getTileWidth());
	// int tileCoordY = (int) ((s.getY() + s.getHeight()) /
	// tmap.getTileHeight());
	// int tileCoordXRightSide = (int) (s.getX() + s.getWidth() /
	// tmap.getTileWidth());
	// int tileCoordYT = (int) (s.getY() / tmap.getTileHeight());
	//
	// //Tile tile = tmap.getTile(tileCoordX, tileCoordY);
	//
	// char ch = tmap.getTileChar(tileCoordX, tileCoordY);
	// char nextch = tmap.getTileChar(tileCoordX+1, tileCoordY-1);
	// char prevch = tmap.getTileChar(tileCoordX, tileCoordY-1);
	//
	// //on the ground
	// if(ch == 'g')
	// {
	// if(s.getVelocityY()>0){
	// s.setVelocityY(0);
	// jumpCount = 0;
	// s.setY(tileCoordY * tmap.getTileHeight() - s.getHeight());
	// }
	// falling = false;
	//
	// }
	// else{ falling = true;}
	//
	// if(ch == 'z' ){
	// tmap.setTileChar('g',0 ,0);
	// score+=1;
	// }
	//
	//
	// if(ch == 'g')
	// {
	// if(s.getVelocityY()>0){
	// s.setVelocityY(0);
	// jumpCount = 0;
	// s.setY(tileCoordY * tmap.getTileHeight() - s.getHeight());
	// }
	// falling = false;
	//
	// }
	// else{ falling = true;}
	//
	// if(nextch == 'g'){
	// if(s.getVelocityX()>0){
	// s.setVelocityX(0);
	// s.setX((tileCoordX * tmap.getTileWidth()) + 12);
	// }
	// }
	//
	// if(prevch == 'g'){
	// if(s.getVelocityX()<0){
	// s.setVelocityX(0);
	// s.setX((tileCoordX * tmap.getTileWidth()) + 64);
	// }
	// }
	//
	// if(s.getX() < 0){
	// s.setX(0);
	// }
	//
	// if(s.getX() + s.getWidth() > tmap.getMapWidth() * tmap.getTileWidth()){
	// s.setX(tmap.getMapWidth() * tmap.getTileWidth() - s.getWidth());
	// }
	//
	// if(ch == 'f'){
	// switchMap();
	// }
	//
	// }

	public void handleTileMapCollisions(Sprite s, long elapsed) // ca
	{

		// the x tile coordinates on the left and right side of the sprite
		int leftTileX = (int) (s.getX() / tmap.getTileWidth());
		int rightTileX = (int) ((s.getX() / tmap.getTileWidth()) + 0.8);

		// the y tile coordinate on the top and bottom of the sprite
		int bottomTileY = (int) ((s.getY() + s.getHeight()) / tmap.getTileHeight());
		int topTileY = (int) (s.getY() / tmap.getTileHeight());

		// the tile characters for each corner of the sprite
		char topRightChar = tmap.getTileChar(rightTileX, topTileY);
		char topLeftChar = tmap.getTileChar(leftTileX, topTileY);
		char bottomRightChar = tmap.getTileChar(rightTileX, bottomTileY);
		char bottomLeftChar = tmap.getTileChar(leftTileX, bottomTileY);

		// System.out.println("topY: " + topTileY);
		// System.out.println("bottomY: " + bottomTileY);
		// System.out.println("leftX: " + leftTileX);
		// System.out.println("rightX: " + rightTileX);

		// bottom side sprite collision
		if (s.getVelocityY() > 0) {
			if (bottomLeftChar == 'g' || bottomRightChar == 'g') {
				// System.out.println("floorCollision");
				s.setVelocityY(0);
				s.setY((bottomTileY * tmap.getTileHeight()) - s.getHeight());
				falling = false;
				// System.out.println("bottomY: " + bottomY);
			}
		} else {
			falling = true;
		}

		// right side sprite collision
		if (s.getVelocityX() > 0) {
			if ((bottomRightChar == 'g' && topRightChar == 'g')) {
				s.setVelocityX(-s.getVelocityX());
				// System.out.println("rightCollision");
				s.setX((rightTileX) * tmap.getTileWidth() - s.getWidth());
				// System.out.println("rightX: " + rightX);
			}
		}

		// left side sprite collision
		if (s.getVelocityX() < 0) {
			if ((bottomLeftChar == 'g' && topLeftChar == 'g')) {
				s.setVelocityX(-s.getVelocityX());
				// System.out.println("leftCollision");
				s.setX(((rightTileX) * tmap.getTileWidth()));
				// System.out.println("leftx: " + leftX);
			}
		}

		// left map boundary
		if (s.getX() < 0) {
			s.setX(0);
		}

		// right map boundary
		if (s.getX() + s.getWidth() > tmap.getMapWidth() * tmap.getTileWidth()) {
			s.setX(tmap.getMapWidth() * tmap.getTileWidth() - s.getWidth());
		}
	}

	// public void handleTileMapCollisions(Sprite s, long elapsed) //ak
	// {
	// int xpTop = (int) (s.getX()/tmap.getTileWidth() + 0.6);
	// int ypTop = (int) ((s.getY() + s.getHeight()) / tmap.getTileHeight());
	//
	// int xpBottom = (int) (s.getX() / tmap.getTileWidth() + 0.5);
	// int ypBottom = (int)((s.getY() + s.getHeight()) / tmap.getTileHeight() -
	// 1.5);
	//
	// int xpLeft = (int) ((s.getX()) / tmap.getTileWidth() +1);
	// int ypLeft = (int) ((s.getY() + s.getHeight()) /
	// tmap.getTileHeight()-0.75 );
	//
	// int xpRight = (int) (s.getX()/tmap.getTileWidth());
	// int ypRight = (int) ((s.getY() + s.getHeight()) / tmap.getTileHeight() -
	// 0.75);
	//
	// if((tmap.getTileChar(xpTop, ypTop) == 'g') && s.getVelocityY() > 0){
	//
	// jumpCount = 0;
	// falling = false;
	// s.setVelocityY(0);
	// s.shiftY(-1);
	// //s.setY((ypBottom ) * tmap.getTileHeight());
	// }
	// else{falling = true;}
	//
	// if(tmap.getTileChar(xpLeft, ypLeft) == 'g' && s.getVelocityX() > 0){
	//
	// s.setVelocityX(0);
	// //s.setX(s.getX() - s.getWidth());
	// }
	//
	// if(tmap.getTileChar(xpRight, ypRight) == 'g' && s.getVelocityX() < 0){
	//
	// s.setVelocityX(0);
	// //s.setX(s.getX());
	// }
	//
	// if(tmap.getTileChar(xpBottom, ypBottom) == 'g'){
	//
	// s.setVelocityX(0);
	// //s.shiftY(2);
	// }
	//
	// if(s.getX() < 0){
	// s.setX(0);
	// s.setVelocityX(0);
	// }
	// if((s.getX() + s.getWidth()) > (tmap.getMapWidth()*
	// tmap.getTileWidth())){
	// s.setX((tmap.getMapWidth()* tmap.getTileWidth()) - s.getWidth());
	// s.setVelocityX(0);
	// }
	// }

	/**
	 * Override of the keyPressed event defined in GameCore to catch our own
	 * events
	 * 
	 * @param e
	 *            The event that has been generated
	 */
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();

		if (key == KeyEvent.VK_ESCAPE)
			stop();

		if (key == KeyEvent.VK_UP)
			up = true;

		if (key == KeyEvent.VK_LEFT)
			left = true;

		if (key == KeyEvent.VK_RIGHT)
			right = true;

		if (key == KeyEvent.VK_P)
			paused = !paused;

		if (key == KeyEvent.VK_S) {
			// Example of playing a sound as a thread
			Sound s = new Sound("sounds/caw.wav");
			s.start();
		}
	}

	/**
	 * when 2 sprite collide, do something
	 * 
	 * * @param sprite s1 and sprite s1
	 */
	public boolean boundingBoxCollision(Sprite s1, Sprite s2) {

		return (((s1.getX() + s1.getWidth() > s2.getX()) && s1.getX() <= s2.getX())
				&& ((s1.getY() + s1.getHeight() > s2.getY()) && s1.getY() < s2.getY()));
	}

	/**
	 * when the key is release set flag to false and break
	 * 
	 */
	public void keyReleased(KeyEvent e) {

		int key = e.getKeyCode();

		// Switch statement instead of lots of ifs...
		// Need to use break to prevent fall through.
		switch (key) {
		case KeyEvent.VK_ESCAPE:
			stop();
			break;
		case KeyEvent.VK_UP:
			up = false;
			break;
		case KeyEvent.VK_DOWN:
			falling = false;
			break;
		case KeyEvent.VK_LEFT:
			left = false;
			break;
		case KeyEvent.VK_RIGHT:
			right = false;
			break;
		default:
			break;
		}
	}

	/**
	 * Switched from level 1 to level 2
	 * 
	 */
	public void switchMap() {
		if (levelNo == 1) { // if on level 1 switch to level 2
			init("level2.txt");
		}

	}

	public void endGame() {

		player.setVelocityX(0);
		player.setVelocityY(0);

		Sound death = new Sound("sounds/death.wav");
		death.start();

	}

}
