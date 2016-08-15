class Task implements Comparable<Task> {

	private int x, y, z;
	private int clientId, sId;

	public Task() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		this.sId = 0;
		this.clientId = 0;
	}

	public Task(int x, int clientId) {
		this.x = x;
		this.y = 0;
		this.z = 0;
		this.sId = 0;
		this.clientId = clientId;
	}

	public Task(Task task) {
		this.x = task.x;
		this.y = task.y;
		this.z = task.z;
		this.clientId = task.clientId;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	public int getSid() {
		return sId;
	}

	public void setsId(int sId) {
		this.sId = sId;
	}

	@Override
	public String toString() {
		return "[x:" + x + ",y:" + y + ",z:" + z + "]";//, client:" + clientId;
	}

	public int compareTo(Task o) {
		if (this.z == o.z) {
			return 0;
		} else if (this.z > o.z) {
			return 1;
		} else {
			return -1;
		}
	}

	@Override
	public int hashCode() { // 0x45d9f3b = Magic Number: 16-bit Avalanche.
		int result = x;
		result = ((result >>> 16) ^ result) * 0x45d9f3b;
		result = ((result >>> 16) ^ result) * 0x45d9f3b;
		result = ((result >>> 16) ^ result);
		return result;
	}

	@Override
	public boolean equals(Object obj) { // Auto Generated.
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Task other = (Task) obj;
		return this.x == other.x;
	}

}
