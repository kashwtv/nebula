#version 150

// Draws a texture at a fixed opacity (used to blend the previous frame for motion blur).

uniform sampler2D Sampler0;
uniform float Alpha;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    fragColor = vec4(texture(Sampler0, texCoord0).rgb, Alpha);
}
