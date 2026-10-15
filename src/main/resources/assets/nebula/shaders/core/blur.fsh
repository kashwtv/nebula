#version 150

// One direction of a separable gaussian blur. Run twice (horizontal, vertical).

uniform sampler2D Sampler0;
uniform vec2 Direction;
uniform float Radius;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    if (Radius < 0.5) {
        fragColor = vec4(texture(Sampler0, texCoord0).rgb, 1.0);
        return;
    }
    float sigma = max(Radius * 0.5, 0.001);
    int r = int(ceil(Radius));
    vec3 sum = vec3(0.0);
    float wsum = 0.0;
    for (int i = -r; i <= r; i++) {
        float fi = float(i);
        float w = exp(-(fi * fi) / (2.0 * sigma * sigma));
        sum += texture(Sampler0, texCoord0 + Direction * fi).rgb * w;
        wsum += w;
    }
    fragColor = vec4(sum / wsum, 1.0);
}
