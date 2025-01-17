package gamestates;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.image.BufferedImage;
import java.util.Random;

import enteties.EnemyManager;
import enteties.Entity;
import enteties.Player;
import levels.LevelManager;
import main.Game;
import objects.ObjectManager;
import ui.GameOverOverlay;
import ui.LevelCompletedOverlay;
import ui.PauseOverlay;
import utilz.LoadSave;

import static utilz.Constants.Enviroment.*;

public class Playing extends State implements Statemethods {
	
	private Player player;
	private LevelManager levelManager;
	private EnemyManager enemyManager;
	private ObjectManager objectManager;
	private PauseOverlay pauseOverlay;
	private GameOverOverlay gameOverOverlay;
	private LevelCompletedOverlay levelCompletedOverlay;
	private boolean paused = false; // show the pause screen or not
	
	private int xLvlOffset;
	//the line which if the player is beyond then we are going to start calculating if there is anything to move. 
	private int leftBorder = (int)(0.2 * Game.GAME_WIDTH); // 0.2 * Game.GAME_WIDTH =>  20% of the game. Meaning if the game was 100 pixels, then if the player is below 20 pixels, we don't need to move the level to the left.            
	private int rightBorder = (int)(0.8 * Game.GAME_WIDTH);
	// with these three variables above, we are going to calculate the max value the offset can have so that level won't move at the end or beginning of the level. 
//	private int lvlTilesWide = LoadSave.GetLevelData()[0].length; // how many tiles entire level in width
//	private int maxTilesOffset = lvlTilesWide - Game.TILES_IN_WIDTH; // the offset from how many tiles we can see. For example, if we can see 20 tiles on a screen, but the level is 30 tiles in width, then we have 10 tiles to work with. 
//	private int maxLvlOffsetX = maxTilesOffset * Game.TILES_SIZE; // turning to pixels. 
	private int maxLvlOffsetX;
	
	private BufferedImage backgroundImg, bigCloud, smallCloud;
	
	private int[] smallCloudsPos; // will contain different y values for small clouds. 
	private Random rnd = new Random(); // for different y values. 
	
	private boolean gameOver;
	private boolean lvlCompleted = false;
	private boolean playerDying;
	
	public Playing(Game game) {
		
		super(game);
		initClasses();
		
		backgroundImg = LoadSave.GetSpriteAtlas(LoadSave.PLAYING_BG_IMG);
		bigCloud = LoadSave.GetSpriteAtlas(LoadSave.BIG_CLOUDS);
		smallCloud = LoadSave.GetSpriteAtlas(LoadSave.SMALL_CLOUDS);
		
		smallCloudsPos = new int[8];
		for (int i = 0; i < smallCloudsPos.length; i++)
			smallCloudsPos[i] = (int)(90 * Game.SCALE) + rnd.nextInt((int)(100 * Game.SCALE)); // smallest we can get is 70. Scale is 2, so 140. 
			
		calcLvlOffset();
		loadStartLevel();
		
	}
	
	public void loadNextLevel() {
		
		levelManager.loadNextLevel();
		player.setSpawn(levelManager.getCurrentLevel().getPlayerSpawn());
		resetAll(); 
		
	}
	
	private void loadStartLevel() {
		
		enemyManager.loadEnemies(levelManager.getCurrentLevel());
		objectManager.loadObjects(levelManager.getCurrentLevel());
		
	}

	private void calcLvlOffset() {
		
		maxLvlOffsetX = levelManager.getCurrentLevel().getLvlOffset();
		
	}

	private void initClasses() {
		
		levelManager = new LevelManager(game);
		enemyManager = new EnemyManager(this);
		objectManager = new ObjectManager(this);
		
		player = new Player (200, 200, (int)(64 * Game.SCALE), (int)(40 * Game.SCALE), this);
		player.loadLvlData(levelManager.getCurrentLevel().getLevelData());
		player.setSpawn(levelManager.getCurrentLevel().getPlayerSpawn());
		
		pauseOverlay = new PauseOverlay(this);
		gameOverOverlay = new GameOverOverlay(this);
		levelCompletedOverlay = new LevelCompletedOverlay(this);
		
	}

	@Override
	public void update() {
		
		if (paused) {
			pauseOverlay.update();
		} else if (lvlCompleted) {
			levelCompletedOverlay.update();
		} else if (gameOver) {
			gameOverOverlay.update();
		} else if (playerDying) {
			player.update();
		} else {
			levelManager.update();
			objectManager.update(levelManager.getCurrentLevel().getLevelData(), player);
			player.update();
			enemyManager.update(levelManager.getCurrentLevel().getLevelData(), player);
			checkCloseToBorder();
		} 
			
	} 

	private void checkCloseToBorder() { // Checking the player position. If the player position is beyond any of these borders, then we do something. 
		
		int playerX = (int)(player.getHitbox().x); // current player x, without the offset
		int diff = playerX - xLvlOffset; 
		
		if (diff > rightBorder) // if playerX is at 85, and the offset is zero, then the diff is 85. If 85 is more than the border (80), then we go 85 - 80 => Offset equals 5. Then, on a next update (if we haven't moved), playerX is at 85 and the Offset is 5, this the diff is 80. Therefore 80 > 80 is false. 
			xLvlOffset += diff - rightBorder;
		else if (diff < leftBorder) 
			xLvlOffset += diff - leftBorder;
		
		// to make sure level offset doesn't get too high and less than 0. 
		if (xLvlOffset > maxLvlOffsetX)
			xLvlOffset = maxLvlOffsetX;
		else if (xLvlOffset < 0)
			xLvlOffset = 0;
			
	}

	@Override
	public void draw(Graphics g) {
		
		g.drawImage(backgroundImg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
		
		drawClouds(g);
		
		levelManager.draw(g, xLvlOffset);
		player.render(g, xLvlOffset);
		enemyManager.draw(g, xLvlOffset);
		objectManager.draw(g, xLvlOffset);
		
		
		if (paused) {
			// to draw a black background that is not fully transparent. 0, 0, 0 means black. 
			g.setColor(new Color(0, 0, 0, 150)); 
			g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
			
			pauseOverlay.draw(g);
			
		} else if (gameOver)
			gameOverOverlay.draw(g);
		else if (lvlCompleted)
			levelCompletedOverlay.draw(g);
		
	}
	
	private void drawClouds(Graphics g) {
		
		for (int i = 0; i < 3; i++)
			g.drawImage(bigCloud, i * BIG_CLOUD_WIDTH - (int)(xLvlOffset * 0.3), (int)(204 * Game.SCALE), BIG_CLOUD_WIDTH, BIG_CLOUD_HEIGHT, null);
		
		for (int i = 0; i < smallCloudsPos.length; i++)
			g.drawImage(smallCloud, SMALL_CLOUD_WIDTH * 4 * i - (int)(xLvlOffset * 0.7), smallCloudsPos[i], SMALL_CLOUD_WIDTH, SMALL_CLOUD_HEIGHT, null); // SMALL_CLOUD_WIDTH * 4 * i meaning that for every index, we are going to add 4 clouds in width between each one. 
		
	}
	
	public void resetAll() { // reset player, enemies, lvl, etc.
		
		gameOver = false;
		paused = false;
		lvlCompleted = false;
		playerDying = false;
		player.resetAll();
		enemyManager.resetAllEnemies();
		objectManager.resetAllObjects();
		
	}
	
	public void setGameOver(boolean gameOver) {
		
		this.gameOver = gameOver;
		
	}
	
	public void checkObjecthit(Rectangle2D.Float attackBox) {
		
		objectManager.checkObjectHit(attackBox);
		
	}
	
	public void checkEnemyHit(Rectangle2D.Float attackBox) {
		
		enemyManager.checkEnemyHit(attackBox);
		
	}
	
	public void checkPotionTouched(Rectangle2D.Float hitbox) {
		
		objectManager.checkObjectTouched(hitbox);
		
	}
	
	public void checkSpikesTouched(Player p) {
		
		objectManager.checkSpikesTouched(p);
		
	}

	public void mouseDragged(MouseEvent e) {
		if (!gameOver)
			if (paused)
				pauseOverlay.mouseDragged(e);
		
	}
	

	@Override
	public void mouseClicked(MouseEvent e) {
		
		if (!gameOver) {
			if (e.getButton() == MouseEvent.BUTTON1) 
				player.setAttacking(true);
			else if (e.getButton() == MouseEvent.BUTTON3)
				player.powerAttack();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (!gameOver) {
			if (paused)
				pauseOverlay.mousePressed(e);
			else if (lvlCompleted)
				levelCompletedOverlay.mousePressed(e);
		} else {
			gameOverOverlay.mousePressed(e);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (!gameOver) {
			if (paused)
				pauseOverlay.mouseReleased(e);
			else if (lvlCompleted)
				levelCompletedOverlay.mouseReleased(e);
		} else {
			gameOverOverlay.mouseReleased(e);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (!gameOver) {
			if (paused)
				pauseOverlay.mouseMoved(e);
			else if (lvlCompleted)
				levelCompletedOverlay.mouseMoved(e);
		} else {
			gameOverOverlay.mouseMoved(e);
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		
		if (gameOver)
			gameOverOverlay.keyPressed(e);
		else			
			switch (e.getKeyCode()) {
			case KeyEvent.VK_A:
				player.setLeft(true);
				break;
			case KeyEvent.VK_D:
				player.setRight(true);
				break;
			case KeyEvent.VK_SPACE:
				player.setJump(true);
				break;
			case KeyEvent.VK_ESCAPE: 
				paused = !paused;
				break;
			}
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (!gameOver)
			switch (e.getKeyCode()) {
			case KeyEvent.VK_A:
				player.setLeft(false);
				break;
			case KeyEvent.VK_D:
				player.setRight(false);
				break;
			case KeyEvent.VK_SPACE:
				player.setJump(false);
				break;
			}
		
	}
	
	public void setLevelCompleted(boolean levelCompleted) {
		
		this.lvlCompleted = levelCompleted;
		if (levelCompleted)
			game.getAudioPlayer().lvlCompleted();
		
	}
	
	public void setMaxLvlOffset(int lvlOffset) {
		
		this.maxLvlOffsetX = lvlOffset;
		
	}
	
	public void unpauseGame() {
		
		paused = false;
		
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public EnemyManager getEnemyManager() {
		
		return enemyManager;
		
	}

	public void windowFocusLost() {
		
		player.resetDirBooleans();
		
	}
	
	public ObjectManager getObjectManager() {
		
		return objectManager;
		
	}
	
	public LevelManager getLevelManager() {
		
		return levelManager;
		
	}

	public void setPlayerDying(boolean playerDying) {
		
		this.playerDying = playerDying;
		
	}

}
