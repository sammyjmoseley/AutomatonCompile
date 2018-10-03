package dk.brics.automaton.compile;

class MaxStackAcum {
    private int currStack;
    private int maxStack;

    public MaxStackAcum() {
        this.currStack = 0;
        this.maxStack = 0;
    }

    public void stackInc(int val) {
        this.currStack += val;
        this.maxStack = Math.max(maxStack, currStack);
    }

    public int getMaxStack() {
        return this.maxStack;
    }
}
