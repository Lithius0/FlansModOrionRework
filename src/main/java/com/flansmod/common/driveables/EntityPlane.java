package com.flansmod.common.driveables;

import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.flansmod.common.FlansMod;
import com.flansmod.common.network.PacketDriveableControl;
import com.flansmod.common.network.PacketPlaneControl;
import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.teams.TeamsManager;
import com.flansmod.common.tools.ItemTool;
import com.flansmod.common.vector.Matrix4f;
import com.flansmod.common.vector.Vector3f;

public class EntityPlane extends EntityDriveable
{
	/**
	 * The max speed for any aircraft. Anything flying faster than this will have their motion dampened
	 * Measured in blocks per tick. Multiply by 20 to get blocks per second.
	 */
	private static final float SPEED_CAP = 5F;
	/**
	 * Acceleration due to gravity
	 * 9.81 meters per second squared
	 * divided by 400 because we need it in meters per tick
	 */
	private static final float GRAVITY = 9.81F / 400F;
	/**
	 * The amount of drag that's imposed at 90 deg angle of attack
	 * 1-MANEUVER_DRAG will be multiplied by the vehicle velocity at 90deg aoa
	 */
	private static final float MANEUVER_DRAG = 0.5F;
	/**
	 * This is a general multiplier for the drag force. The higher this value, the higher the force
	 * Value range: 0-inf, though it is recommend you keep this a low value
	 */
	private static final float DRAG_MULTIPLIER = 0.05F;
	/**
	 * The velocity will multiplied by this instead of the more complex drag function when plane is slow enought (around 4 meters/second)
	 */
	private static final float STATIC_DRAG = 0.95F;
	/**
	 * This value multiplies the user's input for pitch, roll, and yaw controls.
	 * The higher this value, the more maneuverable and generally twitchy the aircrafts tend to be
	 */
	private static final float SENSITIVITY_MULTIPLIER = 0.125F;
	
	/**
	 * The flap positions, used for rendering and for controlling the plane rotations
	 */
	public float flapsYaw, flapsPitchLeft, flapsPitchRight;
	/**
	 * Position of looping engine sound
	 */
	public int soundPosition;
	/**
	 * The angle of the propeller for the renderer
	 */
	public float propAngle;
	/**
	 * Weapon delays
	 */
	public int bombDelay, gunDelay;
	/**
	 * Despawn timer
	 */
	public int ticksSinceUsed = 0;
	/**
	 * Mostly aesthetic model variables. Gear actually has a variable hitbox
	 */
	public boolean varGear = true, varDoor = false, varWing = false;
	/**
	 * Delayer for gear, door and wing buttons
	 */
	public int toggleTimer = 0;
	/**
	 * Current plane mode
	 */
	public EnumPlaneMode mode;
	
	public EntityPlane(World world)
	{
		super(world);
	}
	
	public EntityPlane(World world, double x, double y, double z, PlaneType type, DriveableData data)
	{
		super(world, type, data);
		setPosition(x, y, z);
		prevPosX = x;
		prevPosY = y;
		prevPosZ = z;
		initType(type, true, false);
	}
	
	public EntityPlane(World world, double x, double y, double z, EntityPlayer placer, PlaneType type,
					   DriveableData data)
	{
		this(world, x, y, z, type, data);
		rotateYaw(placer.rotationYaw + 90F);
		rotatePitch(type.restingPitch);
	}
	
	@Override
	public void initType(DriveableType type, boolean firstSpawn, boolean clientSide)
	{
		super.initType(type, firstSpawn, clientSide);
		mode = (((PlaneType)type).mode == EnumPlaneMode.HELI ? EnumPlaneMode.HELI : EnumPlaneMode.PLANE);
	}
	
	@Override
	protected void writeEntityToNBT(NBTTagCompound tag)
	{
		super.writeEntityToNBT(tag);
		tag.setTag("Pos", this.newDoubleNBTList(this.posX, this.posY + 1D, this.posZ));
		tag.setBoolean("VarGear", varGear);
		tag.setBoolean("VarDoor", varDoor);
		tag.setBoolean("VarWing", varWing);
	}
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound tag)
	{
		super.readEntityFromNBT(tag);
		varGear = tag.getBoolean("VarGear");
		varDoor = tag.getBoolean("VarDoor");
		varWing = tag.getBoolean("VarWing");
	}
	
	/**
	 * Called with the movement of the mouse. Used in controlling vehicles if need be.
	 *
	 * @param deltaY
	 * @param deltaX
	 */
	@Override
	public void onMouseMoved(int deltaX, int deltaY)
	{
		if(!FMLCommonHandler.instance().getSide().isClient())
			return;
		if(!FlansMod.proxy.mouseControlEnabled())
			return;
		
		float sensitivity = 0.02F;
		
		flapsPitchLeft -= sensitivity * deltaY;
		flapsPitchRight -= sensitivity * deltaY;
		
		flapsPitchLeft -= sensitivity * deltaX;
		flapsPitchRight += sensitivity * deltaX;
	}
	
	@Override
	public void setPositionRotationAndMotion(double x, double y, double z, float yaw, float pitch, float roll,
											 double motX, double motY, double motZ, float velYaw, float velPitch,
											 float velRoll, float throttle, float steeringYaw)
	{
		super.setPositionRotationAndMotion(x, y, z, yaw, pitch, roll, motX, motY, motZ, velYaw, velPitch, velRoll,
				throttle, steeringYaw);
		flapsYaw = steeringYaw;
	}
	
	@Override
	public boolean processInitialInteract(EntityPlayer entityplayer, EnumHand hand)
	{
		if(isDead)
			return false;
		if(world.isRemote)
			return false;
		
		//If they are using a repair tool, don't put them in
		ItemStack currentItem = entityplayer.getHeldItemMainhand();
		if(currentItem.getItem() instanceof ItemTool && ((ItemTool)currentItem.getItem()).type.healDriveables)
			return true;
		
		PlaneType type = this.getPlaneType();
		//Check each seat in order to see if the player can sit in it
		for(int i = 0; i <= type.numPassengers; i++)
		{
			if(getSeat(i).processInitialInteract(entityplayer, hand))
			{
				if(i == 0)
				{
					bombDelay = type.planeBombDelay;
					FlansMod.proxy.doTutorialStuff(entityplayer, this);
				}
				return true;
			}
		}
		return false;
	}
	
	public boolean serverHandleKeyPress(int key, EntityPlayer player)
	{
		return super.serverHandleKeyPress(key, player);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean pressKey(int key, EntityPlayer player, boolean isOnEvent)
	{
		PlaneType type = this.getPlaneType();
		//Send keys which require server side updates to the server
		boolean canThrust = ((getSeat(0) != null && getSeat(0).getControllingPassenger() instanceof EntityPlayer
				&& ((EntityPlayer)getSeat(0).getControllingPassenger()).capabilities.isCreativeMode)
				|| getDriveableData().fuelInTank > 0) && hasWorkingProp();
		switch(key)
		{
			case 0: //Accelerate : Increase the throttle, up to 1.
			{
				if(canThrust || throttle < 0F)
				{
					throttle += 0.01F;
					if(throttle > 1F)
						throttle = 1F;
				}
				return true;
			}
			case 1: //Decelerate : Decrease the throttle, down to -1, or 0 if the plane cannot reverse
			{
				if(canThrust || throttle > 0F)
				{
					throttle -= 0.01F;
					if(throttle < -1F)
						throttle = -1F;
					if(throttle < 0F && type.maxNegativeThrottle == 0F)
						throttle = 0F;
				}
				return true;
			}
			case 2: //Left : Yaw the flaps left
			{
				flapsYaw -= 4F;
				return true;
			}
			case 3: //Right : Yaw the flaps right
			{
				flapsYaw += 4F;
				return true;
			}
			case 4: //Up : Pitch the flaps up
			{
				flapsPitchLeft += 4F;
				flapsPitchRight += 4F;
				return true;
			}
			case 5: //Down : Pitch the flaps down
			{
				flapsPitchLeft -= 4F;
				flapsPitchRight -= 4F;
				return true;
			}
			case 7: //Inventory : Check to see if this plane allows in-flight inventory editing or if the plane is on the ground
			{
				if(world.isRemote && (type.invInflight || (Math.abs(throttle) < 0.1F && onGround)))
				{
					FlansMod.proxy.openDriveableMenu((EntityPlayer)getSeat(0).getControllingPassenger(), world, this);
				}
				return true;
			}
			case 10: //Change control mode
			{
				FlansMod.proxy.changeControlMode((EntityPlayer)getSeat(0).getControllingPassenger());
				return true;
			}
			case 11: //Roll left
			{
				flapsPitchLeft += 4F;
				flapsPitchRight -= 4F;
				return true;
			}
			case 12: //Roll right
			{
				flapsPitchLeft -= 4F;
				flapsPitchRight += 4F;
				return true;
			}
			case 13: // Gear
			{
				if(toggleTimer <= 0)
				{
					varGear = !varGear;
					player.sendMessage(new TextComponentString("Landing gear " + (varGear ? "down" : "up")));
					toggleTimer = 10;
					FlansMod.getPacketHandler().sendToServer(new PacketDriveableControl(this));
				}
				return true;
			}
			case 14: // Door
			{
				if(toggleTimer <= 0)
				{
					varDoor = !varDoor;
					if(type.hasDoor)
						player.sendMessage(new TextComponentString("Doors " + (varDoor ? "open" : "closed")));
					toggleTimer = 10;
					FlansMod.getPacketHandler().sendToServer(new PacketDriveableControl(this));
				}
				return true;
			}
			case 15: // Wing
			{
				if(toggleTimer <= 0)
				{
					if(type.hasWing)
					{
						varWing = !varWing;
						player.sendMessage(new TextComponentString("Switching mode"));
					}
					if(type.mode == EnumPlaneMode.VTOL)
					{
						if(mode == EnumPlaneMode.HELI)
							mode = EnumPlaneMode.PLANE;
						else mode = EnumPlaneMode.HELI;
						player.sendMessage(new TextComponentString(
								mode == EnumPlaneMode.HELI ? "Entering hover mode" : "Entering plane mode"));
					}
					toggleTimer = 10;
					FlansMod.getPacketHandler().sendToServer(new PacketDriveableControl(this));
				}
				return true;
			}
			case 16: // Trim Button
			{
				axes.setAngles(axes.getYaw(), 0, 0);
				return true;
			}
			default:
			{
				return super.pressKey(key, player, isOnEvent);
			}
		}
	}
	
	@Override
	public void updateKeyHeldState(int key, boolean held)
	{
		super.updateKeyHeldState(key, held);
	}
	
	@Override
	public void onUpdate()
	{
		super.onUpdate();
		
		if(!readyForUpdates)
		{
			return;
		}
		
		//Get plane type
		PlaneType type = getPlaneType();
		DriveableData data = getDriveableData();
		if(type == null)
		{
			FlansMod.log.warn("Plane type null. Not ticking plane");
			return;
		}
		
		//Work out if this is the client side and the player is driving
		boolean thePlayerIsDrivingThis =
				world.isRemote && getSeat(0) != null && getSeat(0).getControllingPassenger() instanceof EntityPlayer
						&& FlansMod.proxy.isThePlayer((EntityPlayer)getSeat(0).getControllingPassenger());
		
		//Despawning
		ticksSinceUsed++;
		if(!world.isRemote && getSeat(0).getControllingPassenger() != null)
			ticksSinceUsed = 0;
		if(!world.isRemote && TeamsManager.planeLife > 0 && ticksSinceUsed > TeamsManager.planeLife * 20)
		{
			setDead();
		}
		
		//Shooting, inventories, etc.
		//Decrement bomb and gun timers
		if(bombDelay > 0)
			bombDelay--;
		if(gunDelay > 0)
			gunDelay--;
		if(toggleTimer > 0)
			toggleTimer--;
		
		//Aesthetics
		//Rotate the propellers
		if(hasEnoughFuel())
		{
			propAngle += (Math.pow(throttle, 0.4)) * 1.5;
		}
		
		//Return the flaps to their resting position
		flapsYaw *= 0.9F;
		flapsPitchLeft *= 0.9F;
		flapsPitchRight *= 0.9F;
		
		//Limit flap angles
		if(flapsYaw > 20)
			flapsYaw = 20;
		if(flapsYaw < -20)
			flapsYaw = -20;
		if(flapsPitchRight > 20)
			flapsPitchRight = 20;
		if(flapsPitchRight < -20)
			flapsPitchRight = -20;
		if(flapsPitchLeft > 20)
			flapsPitchLeft = 20;
		if(flapsPitchLeft < -20)
			flapsPitchLeft = -20;
		
		//Player is not driving this. Update its position from server update packets 
		if(world.isRemote && !thePlayerIsDrivingThis)
		{
			//The driveable is currently moving towards its server position. Continue doing so.
			if(serverPositionTransitionTicker > 0)
			{
				moveTowardServerPosition();
			}
			//If the driveable is at its server position and does not have the next update, it should just simulate itself as a server side plane would, so continue
		}
		
		//Movement
		
		//Throttle handling
		//Without a player, default to 0
		//With a player default to 0.5 for helicopters (hover speed)
		//And default to the range 0.25 ~ 0.5 for planes (taxi speed ~ take off speed)
		float throttlePull = 0.99F;
		if(getSeat(0) != null && getSeat(0).getControllingPassenger() != null && mode == EnumPlaneMode.HELI &&
				canThrust())
			throttle = (throttle - 0.5F) * throttlePull + 0.5F;

		float currentSpeed = Math.min((float)getSpeedXYZ(), SPEED_CAP);
		
		//Alter angles
		//Sensitivity function
		float sensitivityAdjust = SENSITIVITY_MULTIPLIER * (float)Math.sqrt(throttle) + 0.01F;
		
		
		float yaw = flapsYaw * (flapsYaw > 0 ? type.turnLeftModifier : type.turnRightModifier) * sensitivityAdjust;
		
		//Pitch according to the sum of flapsPitchLeft and flapsPitchRight / 2
		float flapsPitch = (flapsPitchLeft + flapsPitchRight) / 2F;
		float pitch = flapsPitch * (flapsPitch > 0 ? type.lookUpModifier : type.lookDownModifier) * sensitivityAdjust;
		
		//Roll according to the difference between flapsPitchLeft and flapsPitchRight / 2
		float flapsRoll = (flapsPitchRight - flapsPitchLeft) / 2F;
		float roll = flapsRoll * (flapsRoll > 0 ? type.rollLeftModifier : type.rollRightModifier) * sensitivityAdjust;
		
		//Damage modifiers
		if(mode == EnumPlaneMode.PLANE)
		{
			if(!isPartIntact(EnumDriveablePart.tail))
			{
				yaw = 0;
				pitch = 0;
				roll = 0;
			}
			if(!isPartIntact(EnumDriveablePart.leftWing))
				roll -= 7F * getSpeedXZ();
			if(!isPartIntact(EnumDriveablePart.rightWing))
				roll += 7F * getSpeedXZ();
		}
		
		axes.rotateLocalYaw(yaw);
		axes.rotateLocalPitch(pitch);
		axes.rotateLocalRoll(-roll);
		
		if(world.isRemote && !FlansMod.proxy.mouseControlEnabled())
		{
			//axes.rotateGlobalRoll(-axes.getRoll() * 0.1F);
		}
		
		//The power output of the engine will determine the speed of the plane and its acceleration
		float enginePower = 0.01F * type.maxThrottle * (data.engine == null ? 0 : data.engine.engineSpeed);
		
		if(!canThrust())
			enginePower = 0;
		
		int numPropsWorking = 0;
		int numProps = 0;
		
		switch(mode)
		{
			case HELI:
				
				//Count the number of working propellers
				for(Propeller prop : type.heliPropellers)
					if(isPartIntact(prop.planePart))
						numPropsWorking++;
				numProps = type.heliPropellers.size();
				
				Vector3f up = axes.getYAxis();
				
				float effectiveEnginePower = enginePower * (numProps == 0 ? 0 : (float)numPropsWorking / numProps);

				//Apply gravity
				motionY -= GRAVITY;
				float upwardsForce = throttle * effectiveEnginePower + (GRAVITY - effectiveEnginePower);
				
				if(!isPartIntact(EnumDriveablePart.blades))
				{
					upwardsForce = 0F;
				}
				
				//Move up
				//Throttle - 0.5 means that the positive throttle scales from -0.5 to +0.5. Thus it accounts for gravity-ish
				motionX += upwardsForce * up.x * 0.5F;
				motionY += upwardsForce * up.y;
				motionZ += upwardsForce * up.z * 0.5F;
				
				break;
			
			case PLANE:

				
				//Apply forces
				Vector3f forwards = (Vector3f)axes.getXAxis().normalise();

				//Count the number of working propellers
				for(Propeller prop : type.propellers)
					if(isPartIntact(prop.planePart))
						numPropsWorking++;
				numProps = type.propellers.size();
				
				//The proportion of propellors that are able to produce thrust, essentially a multiplier
				//Value range is 0-1
				float workingPropellorProportion = (numProps == 0 ? 0 : (float)numPropsWorking / numProps);
				
				/*
				 * This is a really simply way of making sure the plane is going where it is pointing
				 * Real aircraft do this with clever aerodynamics and lift forces from the airfoils
				 * To simplify things a bit, we're just going to interpolate between the velocity and facing vector
				 * 
				 * NOTE: This interpolation step should go before any other modifications to motion
				 * Otherwise they will get erroneously added to the currentSpeed
				 * 
				 * I know this is kind of terrible, but I don't have the time to implement something more accurate
				 * and there were some issues with the previous implementation that I couldn't live with -Lithius
				 */
				
				//Interpolation alpha
				float alpha = throttle * workingPropellorProportion * 0.5F;

				//A currentSpeed of 0 will give us a NaN error here.
				//The throttle check is to prevent a weird bug regarding the fact that the velocity from the gravity carries to the next tick. 
				//Even if the aircraft is on the ground
				//The other checks are to make sure there is a pilot before we do the checks
				if (currentSpeed > 0.1 && throttle > 0.1 && getSeat(0) != null && getSeat(0).getControllingPassenger() != null) {
					Vector3f targetVector = new Vector3f(forwards.x, forwards.y, forwards.z);
					targetVector.scale(currentSpeed);
					
					float angleOfAttack = Vector3f.angle(targetVector, new Vector3f(motionX, motionY, motionZ));
					
					float maneuverDrag = (float) (1F - (angleOfAttack * MANEUVER_DRAG / Math.PI));
	
					targetVector.scale(maneuverDrag);
					
					//Interpolating motion
					motionX = (1F - alpha) * motionX + alpha * targetVector.x;
					motionY = (1F - alpha) * motionY + alpha * targetVector.y;
					motionZ = (1F - alpha) * motionZ + alpha * targetVector.z;
				}
				//Apply gravity
				motionY -= GRAVITY;
				
				/**
				 * Lift is implemented very simply. It is a force that directly counteracts gravity, no matter the orientation.
				 * This means that all planes can effectively become helicopters
				 */
				
				//The 0.7F is there to limit the max lift to 70% of the gravity
				float amountOfLift = 0.7F * GRAVITY * throttle;
				
				//Missing wings will reduce lift
				int numWingsIntact = 0;
				if(isPartIntact(EnumDriveablePart.rightWing)) numWingsIntact++;
				if(isPartIntact(EnumDriveablePart.leftWing)) numWingsIntact++;
				
				amountOfLift *= numWingsIntact / 2F;
				
				if(!isPartIntact(EnumDriveablePart.tail))
					amountOfLift *= 0.75F;

				//Capping lift at the amount of gravity
				if(amountOfLift > GRAVITY)
					amountOfLift = GRAVITY;
				
				motionY += amountOfLift;
				
				//The acceleration the plane would undergo if there was no drag
				//This is not net acceleration, the plane's velocity at the end of the tick still has to go through drag and velocity capping
				float planeAcceleration = throttle * enginePower * workingPropellorProportion;
				
				motionX += planeAcceleration * forwards.x;
				motionY += planeAcceleration * forwards.y;
				motionZ += planeAcceleration * forwards.z;

				break;
			default:
				break;
		}
		
		//Apply drag
		if (currentSpeed > 0.2F) 
		{
			motionX -= Math.signum(motionX) * (motionX * motionX) * type.drag * DRAG_MULTIPLIER;
			motionY -= Math.signum(motionY) * (motionY * motionY) * type.drag * DRAG_MULTIPLIER;
			motionZ -= Math.signum(motionZ) * (motionZ * motionZ) * type.drag * DRAG_MULTIPLIER;
		}
		else
		{
			motionX *= STATIC_DRAG;
			motionY *= STATIC_DRAG;
			motionZ *= STATIC_DRAG;
		}
		
		
		//Burning fuel
		data.fuelInTank -= throttle * data.engine.fuelConsumption * FlansMod.globalFuelUseMultiplier;
		
		//Velocity capping
		double planeSpeed = this.getSpeed();
		if(planeSpeed > SPEED_CAP)
		{
			//Scaling the motion vector so the resultant vector's magnitude should equal SPEED_CAP
			motionX *= SPEED_CAP / planeSpeed;
			motionY *= SPEED_CAP / planeSpeed;
			motionZ *= SPEED_CAP / planeSpeed;
		}
		
		for(EntityWheel wheel : wheels)
		{
			if(wheel != null && world != null)
			{
				wheel.prevPosX = wheel.posX;
				wheel.prevPosY = wheel.posY;
				wheel.prevPosZ = wheel.posZ;
			}
		}
		for(EntityWheel wheel : wheels)
		{
			if(wheel != null && world != null)
				if(type.floatOnWater && world.containsAnyLiquid(wheel.getEntityBoundingBox()))
				{
					motionY += type.buoyancy;
				}
		}
		
		//Move the wheels first
		for(EntityWheel wheel : wheels)
		{
			if(wheel != null)
			{
				wheel.prevPosY = wheel.posY;
				wheel.move(MoverType.SELF, motionX, motionY, motionZ);
			}
		}
		
		//Update wheels
		for(int i = 0; i < 2; i++)
		{
			Vector3f amountToMoveCar = new Vector3f(motionX / 2F, motionY / 2F, motionZ / 2F);
			
			for(EntityWheel wheel : wheels)
			{
				if(wheel == null)
					continue;
				
				//Hacky way of forcing the car to step up blocks
				onGround = true;
				wheel.onGround = true;
				
				//Update angles
				wheel.rotationYaw = axes.getYaw();
				
				//Pull wheels towards car
				Vector3f targetWheelPos = axes.findLocalVectorGlobally(
						getPlaneType().wheelPositions[wheel.getExpectedWheelID()].position);
				Vector3f currentWheelPos = new Vector3f(wheel.posX - posX, wheel.posY - posY, wheel.posZ - posZ);
				
				float targetWheelLength = targetWheelPos.length();
				float currentWheelLength = currentWheelPos.length();
				
				if(currentWheelLength > targetWheelLength * 3.0d)
				{
					// Make wheels break?
					//this.attackPart(EnumDriveablePart.backLeftWheel, source, damage);
				}
				
				float dLength = targetWheelLength - currentWheelLength;
				float dAngle = Vector3f.angle(targetWheelPos, currentWheelPos);
				
				{
					//Now Lerp by wheelSpringStrength and work out the new positions		
					float newLength = currentWheelLength + dLength * type.wheelSpringStrength;
					Vector3f rotateAround = Vector3f.cross(targetWheelPos, currentWheelPos, null);
					
					rotateAround.normalise();
					
					Matrix4f mat = new Matrix4f();
					mat.m00 = currentWheelPos.x;
					mat.m10 = currentWheelPos.y;
					mat.m20 = currentWheelPos.z;
					mat.rotate(dAngle * type.wheelSpringStrength, rotateAround);
					
					axes.rotateGlobal(-dAngle * type.wheelSpringStrength, rotateAround);
					
					Vector3f newWheelPos = new Vector3f(mat.m00, mat.m10, mat.m20);
					newWheelPos.normalise().scale(newLength);
					
					//The proportion of the spring adjustment that is applied to the wheel. 1 - this is applied to the plane
					float wheelProportion = 0.75F;
					
					//wheel.motionX = (newWheelPos.x - currentWheelPos.x) * wheelProportion;
					//wheel.motionY = (newWheelPos.y - currentWheelPos.y) * wheelProportion;
					//wheel.motionZ = (newWheelPos.z - currentWheelPos.z) * wheelProportion;
					
					Vector3f amountToMoveWheel = new Vector3f();
					
					amountToMoveWheel.x = (newWheelPos.x - currentWheelPos.x) * (1F - wheelProportion);
					amountToMoveWheel.y = (newWheelPos.y - currentWheelPos.y) * (1F - wheelProportion);
					amountToMoveWheel.z = (newWheelPos.z - currentWheelPos.z) * (1F - wheelProportion);
					
					amountToMoveCar.x -= (newWheelPos.x - currentWheelPos.x) * (1F - wheelProportion);
					amountToMoveCar.y -= (newWheelPos.y - currentWheelPos.y) * (1F - wheelProportion);
					amountToMoveCar.z -= (newWheelPos.z - currentWheelPos.z) * (1F - wheelProportion);
					
					//The difference between how much the wheel moved and how much it was meant to move. i.e. the reaction force from the block
					//amountToMoveCar.x += ((wheel.posX - wheel.prevPosX) - (motionX)) * 0.616F / wheels.length;
					amountToMoveCar.y += ((wheel.posY - wheel.prevPosY) - (motionY)) * 0.5F / wheels.length;
					//amountToMoveCar.z += ((wheel.posZ - wheel.prevPosZ) - (motionZ)) * 0.0616F / wheels.length;
					
					if(amountToMoveWheel.lengthSquared() >= 32f * 32f)
					{
						FlansMod.log.warn("Wheel tried to move " + amountToMoveWheel.length() + " in a single frame, capping at 32 blocks");
						amountToMoveWheel.normalise();
						amountToMoveWheel.scale(32f);
					}
					
					wheel.move(MoverType.SELF, amountToMoveWheel.x, amountToMoveWheel.y, amountToMoveWheel.z);
				}
			}
			
			move(MoverType.SELF, amountToMoveCar.x, amountToMoveCar.y, amountToMoveCar.z);
			
		}
		
		checkForCollisions();
		
		//Sounds
		//Starting sound
		if(throttle > 0.01F && throttle < 0.2F && soundPosition == 0 && hasEnoughFuel())
		{
			PacketPlaySound.sendSoundPacket(posX, posY, posZ, FlansMod.soundRange, dimension, type.startSound, false);
			soundPosition = type.startSoundLength;
		}
		//Flying sound
		if(throttle > 0.2F && soundPosition == 0 && hasEnoughFuel())
		{
			PacketPlaySound.sendSoundPacket(posX, posY, posZ, FlansMod.soundRange, dimension, type.engineSound, false);
			soundPosition = type.engineSoundLength;
		}
		
		//Sound decrementer
		if(soundPosition > 0)
			soundPosition--;
		
		for(EntitySeat seat : getSeats())
		{
			if(seat != null)
				seat.updatePosition();
		}
		
		//Calculate movement on the client and then send position, rotation etc to the server
		if(serverPosX != posX || serverPosY != posY || serverPosZ != posZ || serverYaw != axes.getYaw())
		{
			if(thePlayerIsDrivingThis)
			{
				FlansMod.getPacketHandler().sendToServer(new PacketPlaneControl(this));
				serverPosX = posX;
				serverPosY = posY;
				serverPosZ = posZ;
				serverYaw = axes.getYaw();
			}
		}
		
		PostUpdate();
	}
	
	public boolean canThrust()
	{
		return (getSeat(0) != null && getSeat(0).getControllingPassenger() instanceof EntityPlayer
				&& ((EntityPlayer)getSeat(0).getControllingPassenger()).capabilities.isCreativeMode) ||
				driveableData.fuelInTank > 0;
	}
	
	@Override
	public void setDead()
	{
		super.setDead();
	}
	
	@Override
	public boolean gearDown()
	{
		return varGear;
	}
	
	private boolean hasWorkingProp()
	{
		PlaneType type = getPlaneType();
		if(type.mode == EnumPlaneMode.HELI || type.mode == EnumPlaneMode.VTOL)
			for(Propeller prop : type.heliPropellers)
				if(isPartIntact(prop.planePart))
					return true;
		if(type.mode == EnumPlaneMode.PLANE || type.mode == EnumPlaneMode.VTOL)
			for(Propeller prop : type.propellers)
				if(isPartIntact(prop.planePart))
					return true;
		return false;
	}
	
	public boolean attackEntityFrom(DamageSource damagesource, float i, boolean doDamage)
	{
		if(world.isRemote || isDead)
			return true;
		
		PlaneType type = PlaneType.getPlane(driveableType);
		
		if(damagesource.damageType.equals("player") && damagesource.getTrueSource().onGround
				&& (getSeat(0) == null || getSeat(0).getControllingPassenger() == null))
		{
			ItemStack planeStack = new ItemStack(type.item, 1, driveableData.paintjobID);
			NBTTagCompound tags = new NBTTagCompound();
			planeStack.setTagCompound(tags);
			driveableData.writeToNBT(tags);
			entityDropItem(planeStack, 0.5F);
			setDead();
		}
		return true;
	}
	
	@Override
	public boolean canHitPart(EnumDriveablePart part)
	{
		return varGear || (part != EnumDriveablePart.coreWheel && part != EnumDriveablePart.leftWingWheel &&
				part != EnumDriveablePart.rightWingWheel && part != EnumDriveablePart.tailWheel);
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource damagesource, float i)
	{
		return attackEntityFrom(damagesource, i, true);
	}
	
	public PlaneType getPlaneType()
	{
		return PlaneType.getPlane(driveableType);
	}
	
	@Override
	protected void dropItemsOnPartDeath(Vector3f midpoint, DriveablePart part)
	{
	}
	
	@Override
	public String getBombInventoryName()
	{
		return "Bombs";
	}
	
	@Override
	public String getMissileInventoryName()
	{
		return "Missiles";
	}
	
	@Override
	public boolean hasMouseControlMode()
	{
		return true;
	}
}
