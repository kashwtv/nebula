#version 150

// Anti-aliased rounded rectangle via signed distance field.
// Pad > 0  -> soft glow / drop shadow spreading Pad units outside the rect.
// Thickness > 0 -> outline ring only.

uniform vec2 Size;
uniform float Radius;
uniform float Thickness;
uniform float Pad;
uniform float Smooth;
uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float sdRoundRect(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 full = Size + 2.0 * Pad;
    vec2 p = (texCoord0 - 0.5) * full;
    float d = sdRoundRect(p, Size * 0.5, Radius);
    float a;
    if (Pad > 0.0) {
        float t = clamp(d / Pad, 0.0, 1.0);
        a = pow(1.0 - t, 2.4);
    } else {
        if (Thickness > 0.0) d = abs(d + Thickness * 0.5) - Thickness * 0.5;
        a = 1.0 - smoothstep(-Smooth, Smooth, d);
    }
    vec4 c = vertexColor * ColorModulator;
    c.a *= a;
    if (c.a <= 0.002) discard;
    fragColor = c;
}
