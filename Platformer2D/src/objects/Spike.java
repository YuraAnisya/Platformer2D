package objects;

import main.Game;

public class Spike extends GameObject{

	public Spike(int x, int y, int objType) {
		
		super(x, y, objType);
		
		initHitbox(32, 16);
		xDrawOffset = 0;
		yDrawOffset = (int)(16 * Game.SCALE);
		hitbox.y += yDrawOffset; // to put them on a floor. 
		
	}

}
