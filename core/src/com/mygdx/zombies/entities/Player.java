package com.mygdx.zombies.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.mygdx.zombies.InfoContainer;
import com.mygdx.zombies.Zombies;
import com.mygdx.zombies.items.MeleeWeapon;
import com.mygdx.zombies.items.PowerUp;
import com.mygdx.zombies.items.RangedWeapon;
import com.mygdx.zombies.items.Weapon;
import com.mygdx.zombies.states.Level;
import com.mygdx.zombies.states.StateManager;

/**
 * The main player class
 */
public class Player extends Entity {

	//#changed4 added a player type enum rather than using an integer to identify player types
	public enum PlayerType { COMPSCI, CHEMISTRY, FOOTBALLER }
	public static Float health;
	private static int points;
	private static Float counter;
	private static String pointDisplay;
	private Sprite sprite;
	private double angleRads;
	private float angleDegrees;
	private long last;
	private Sprite hud;
	private SpriteBatch spriteBatch;
	private SpriteBatch UIBatch;
	private byte swingStep;
	private byte swingDirection;
	private PowerUp powerUp;
	public static Weapon weapon;
	private static PlayerType type;
	private int charStealth;
	private int charSpeed;
	private float charDamage;
	private static Texture equippedTexture;
	private static Texture unequippedTexture;
	private static Texture zombieTexture;
	private static Texture antidoteTexture;
	private static Level level;
	//added for assessment 3
	private boolean gateOpen;

	private float deathMarker = 0;
	private boolean isZombie = false;
	private int zombieCountdown = 20;

	/** Constructor for the player class
	 * @param level - the level instance to spawn the player in
	 * @param x - the x spawn coordinate
	 * @param y - the y spawn coordinate
	 */
	public Player(Level level, int x, int y) {
		spriteBatch = level.getWorldBatch();
		UIBatch = level.getUIBatch();
		Player.level = level;

		//Initialise player attributes to default values
		charDamage = charSpeed = charStealth = 1;
		
		//Set player attributes based on the current type
		switch(type) {
			case COMPSCI:
				charDamage = 0.5f;
				break;
			case CHEMISTRY:
				charStealth = 2;
				break;
			case FOOTBALLER:
				charSpeed = 2;
				break;
		}
		
		//Initialise player health if not yet set in previous stage
		if(health == null || health <= 0) {
			health = 10f;
		}
		
		//Initialise time if not yet set in previous stage
		if(counter == null) {
			counter = (float) 0;
			pointDisplay = "0";
		}


		//Load player textures
		loadTextures();
		hud = new Sprite(new Texture(Gdx.files.internal("player/heart.png")));
		sprite = new Sprite(unequippedTexture);

        antidoteTexture = new Texture("pickups/antidote.png");
		
	
		//Update texture if set
		if(weapon != null) {
			weapon.setLevel(level);
			if(weapon instanceof RangedWeapon) {
				setEquippedTexture();
			}		
		}
	
		//Generate Box2D object
		FixtureDef fixtureDef = new FixtureDef() {
			{
				density = 40;
				friction = 0.5f;
				restitution = 0f;
				filter.categoryBits = Zombies.playerFilter;
				//#changed4 now using bitwise OR statements to calculate the collision mask, rather than having a manual calculation
				filter.maskBits = (short) (Zombies.zombieFilter | Zombies.zombieProjectileFilter | Zombies.wallFilter
						| Zombies.pickupFilter | Zombies.gateFilter | Zombies.npcFilter);
			}
		};		
		GenerateBodyFromSprite(level.getBox2dWorld(), sprite, InfoContainer.BodyID.PLAYER, fixtureDef);
		body.setTransform(x / Zombies.PhysicsDensity, y / Zombies.PhysicsDensity, 0);
		body.setLinearDamping(20);
		body.setFixedRotation(true);

		//Initialise variables for the arm swinging capability
		swingStep = 0;
		swingDirection = -1;	
	}
	
	/**
	 * Loads player textures based on type
	 */
	private static void loadTextures() {
		//#changed4 updated to work with the new player type enum rather than a player type integer
		int typeNumber = type.ordinal()+1;
		equippedTexture = new Texture(Gdx.files.internal("player/player" + typeNumber + "_equipped.png"));
		unequippedTexture = new Texture(Gdx.files.internal("player/player" + typeNumber + "_unequipped.png"));
		zombieTexture = new Texture(Gdx.files.internal("zombie/ZombiePlayer.png"));
	}
	
	/** Set the player type and update textures accordingly
	 * @param type - the player type to set to
	 */
	public static void setType(PlayerType type) {
		Player.type = type;
		loadTextures();
	}
	
	/**
	 * Show the weapon equipped texture
	 */
	private void setEquippedTexture() {
		sprite.setTexture(equippedTexture);
	}
	
	/**
	 * Show the weapon unequipped texture
	 */
	private void setUnequippedTexture() {
		sprite.setTexture(unequippedTexture);
	}
	
	/**Set the current weapon, updating the player texture appropriately
	 * @param weapon - the weapon to equip
	 */
	public void setWeapon(Weapon weapon) {
		//If weapon is melee type, do not use equipped texture
		if(weapon instanceof MeleeWeapon)
			setUnequippedTexture();
		else
			setEquippedTexture();
		//Play equip sound
		Zombies.soundAmmo.play();
		Player.weapon = weapon;
	}

	/**
	 * @return true if player has a weapon, false otherwise
	 */
	private boolean hasWeapon() {
		return weapon != null;
	}
	
	/**
	 * @return the relative rotation of the player's hands in degrees
	 */
	private float getHandsRotation() {
		return swingStep*15;
	}

    /**
     * @return whether the player's hands are at the critical position necessary to inflict a damaging melee attack
     * #changed4 added this method
     */
	public boolean isSwingAtCriticalPoint() {
	    return weapon instanceof MeleeWeapon && swingStep >= 2 && swingStep <= 8;
    }
	
	/**
	 * @return the relative position of the player's hands
	 */
	public Vector2 getHandsPosition() {				
		double rot = angleRads + swingStep/3.f;
		float x = (float)Math.cos(rot)*65f;
		float y = (float)Math.sin(rot)*65f;
		
		return new Vector2(x, y);
	}
	
	/**
	 * Update the number of counter and the display value
	 * The counter should gradually increase at a varying speed
	 */
	private void updateCounter() {
		long timer = System.nanoTime()/500000000;
		
		if (timer % 2 == 0 && timer != last) {							
			counter += 1; /*Math.round(Math.random() * 100);*/
			
			if(counter <= 0) {
				counter = (float) 0;
			}	
			
			pointDisplay = Integer.toString(Math.round(counter));
			if (counter == 1) {
			    pointDisplay += " second";
            } else {
			    pointDisplay += " seconds";
            }
			last = timer;
		}
	}

	/**
	 * added for assessment 3
	 * checks if the level is finished and opens the gate to next level if so
	 */
	private void checkGate(){
		if (!gateOpen && level.isGateOpen())
			gateOpen = true;
	}

	//code for assessment 3
    public void closeGate(){ gateOpen = false;}

	/**
	 * added for assessment 3
	 * @return true if the current level is finished
	 */
	public boolean getGate(){
		return gateOpen;
	}

	/**
	 * @return the amount of noise generated by the player accounting for stealth and speed
	 */
	double getNoise() {
		int stealth = powerUp==null ? 1 : powerUp.getStealthBoost()+1;
		double noise = body.getLinearVelocity().len() / (stealth*charStealth) * 250;
		
		//If current weapon is a ranged weapon and is firing, make a lot of noise
		if(weapon instanceof RangedWeapon) {
			RangedWeapon rweapon = (RangedWeapon)weapon;
			if(rweapon.isFiring()) {
				noise += 2000;
			}
		}
		
		return noise;
	}

	/**
	 * @param mouseCoords - the mouse coordinates to look towards
	 */
	private void look(Vector3 mouseCoords) {
		//Calculate angle between player and mouse
		angleRads = Zombies.angleBetweenRads(new Vector2(getPositionX(), getPositionY()),
				new Vector2(mouseCoords.x, mouseCoords.y)) + Math.PI/2;
		angleDegrees = (float) Math.toDegrees(angleRads);
		//Update sprite rotation
		sprite.setRotation(angleDegrees);
	}

	/**
	 * Poll keyboard and move player according to presses
	 * Takes into account player speed power up and speed attribute
	 */
	private void move() {
		int speedBoost = powerUp==null ? 1 : powerUp.getSpeedBoost();
		Vector2 playerPosition = body.getPosition();
		
		if (Gdx.input.isKeyPressed(Keys.W))
			body.applyLinearImpulse(new Vector2(0, 25*speedBoost*charSpeed), playerPosition, true);
		else if (Gdx.input.isKeyPressed(Keys.S))
			body.applyLinearImpulse(new Vector2(0, -25*speedBoost*charSpeed), playerPosition, true);

		if (Gdx.input.isKeyPressed(Keys.A))
			body.applyLinearImpulse(new Vector2(-25*speedBoost*charSpeed, 0), playerPosition, true);
		else if (Gdx.input.isKeyPressed(Keys.D))
			body.applyLinearImpulse(new Vector2(25*speedBoost*charSpeed, 0), playerPosition, true);
	}
	
	/**
	 * @return true if hands are moving
	 */
	public boolean isSwinging() {
		return weapon instanceof MeleeWeapon && swingStep > 0 && swingStep < 10;
	}

	/** Updates player position, rotation, score and weaponry
	 * @param mouseCoords - the mouse coordinates to rotate the player towards
	 */
	public void update(Vector3 mouseCoords) {
		
		if (weapon != null) {
			Vector2 h = getHandsPosition();
			Vector2 pos = new Vector2(getPositionX() + h.x - 30, getPositionY() + h.y - 30);
			if(weapon instanceof MeleeWeapon)
				swingUpdate();
			else
				swingStep=5;
			weapon.update((int)(pos.x),
					(int)(pos.y), angleDegrees+getHandsRotation());
			if (Gdx.input.isButtonPressed(Buttons.LEFT))
				weapon.use();
		}	
		
		move();
		look(mouseCoords);
		updateCounter();
		checkGate();
		
		sprite.setPosition(getPositionX() - sprite.getWidth() / 2, getPositionY() - sprite.getHeight() / 2);
		updateCounter();
		
		if (deathMarker > 0) {	
			if(health > 0) {
				setZombie(false);
			}
			else if(counter>deathMarker+zombieCountdown) {
				deathMarker = 0;
				isZombie = false;

				//Sometimes collisions can occur after the end screen is shown if Box2D engine is taking a while to dispose of bodies
				if(StateManager.getCurrentState() instanceof Level)
				    StateManager.loadState(StateManager.StateID.UDIED, level.isAliveNPC());
			}
		}	
	}

	/**
	 * Method to deal with hand movement update
	 */
	private void swingUpdate() {
		if(isSwinging())
			swingStep += swingDirection;
		else {
				if(Gdx.input.isButtonPressed(Buttons.LEFT)){
					Zombies.soundSwing.play();
					swingDirection *= -1;
					swingStep+=swingDirection;	
			}
		}		
	}

	/**
	 * Draw the player sprite and weapon
	 */
	public void render() {
		sprite.draw(spriteBatch);
		
		if(weapon != null) {
			weapon.render();
		}
	}

	/**
	 * Draw the player HUD
	 */
	public void hudRender() {
		Zombies.pointsFont.draw(UIBatch, "Time spent: " + pointDisplay, 800, 700);
		Zombies.pointsFont.draw(UIBatch, "Points: " + getPoints(), 888, 650);
		
		if(isZombie) {
			Zombies.pointsFont.draw(UIBatch, "You have " + (zombieCountdown-(counter-deathMarker)) + " seconds"
										   + "\nto find the antidote ", 450, 600);
			
			UIBatch.draw(antidoteTexture, 830, 480);
		}
		
		for (int i = 0; i < health; i++) {
			hud.setPosition(100 + i * 50, 620);
			hud.draw(UIBatch);
		}
	}
	
	public Body getBody() {
		return body;
	}

	public double getAngleRadians() {
		return angleRads;
	}

	/** Give the player a power up, replacing the previous one
	 * @param powerUp - the power up to apply to the player
	 */
	public void setPowerUp(PowerUp powerUp) {
		Zombies.soundPowerUp.play();
		this.powerUp = powerUp;
		health += powerUp.getHealthBoost();
	}

	public Float getHealth() {
		return health;
	}

	/** Sets the health to the given value, restarting the current level if depleted
	 * @param health - the value to set the health to
	 */
	public void setHealth(float health) {
		Player.health = health;
		//Restart current level from last entry point if health depleted

		if(health <= 0){
			points -= 150;
			setZombie(true);
		}
	}
	
	public void setZombie(boolean isZombie) {
		this.isZombie = isZombie;
		
		if(isZombie) {
			sprite.setTexture(zombieTexture);
			deathMarker = counter;
			charSpeed = -charSpeed;
			//Added for assessment 3
			//All weapons should be dropped before restarting
			if(hasWeapon()) {
				weapon = null;
			}
		}	
		else {
			sprite.setTexture(unequippedTexture);
			deathMarker = 0;
			zombieCountdown = -1;
			setHealth(10);
			charSpeed = -charSpeed;
		}
	}

	public static int getPoints() {
		return points;
	}

	public static void setPoints(int points) {
		Player.points = points;
	}

	public static Float getCounter() {
		return counter;
	}

	public static void setCounter(Float counter) {
		Player.counter = counter;
	}

	public boolean isZombie() {
		return isZombie;
	}
	
	// If player has been zombie for x seconds,
	// random zombie can drop antidote
	public boolean dropAnti() {
		return counter > deathMarker + 5;
	}

	public float getDamage() {
		return charDamage;
	}

	/*
	 * Dispose of the player instance, clearing the memory
	 */
	public void dispose() {
		super.dispose();
		hud.getTexture().dispose();
		equippedTexture.dispose();
		unequippedTexture.dispose();
	}
}
