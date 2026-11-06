#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 vUv;
in vec4 vColor;
flat in ivec2 vSize;
flat in ivec2 vRadii;
in float vLine;

out vec4 fragColor;

// Odległość ze znakiem do zaokrąglonego prostokąta o czterech różnych promieniach.
// p: punkt względem środka (y w dół), hs: połowa rozmiaru, r: (tl, tr, br, bl).
float sdRoundBox(vec2 p, vec2 hs, vec4 r) {
    float rr = (p.x > 0.0) ? ((p.y > 0.0) ? r.z : r.y) : ((p.y > 0.0) ? r.w : r.x);
    vec2 q = abs(p) - hs + rr;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rr;
}

void main() {
    vec2 size = vec2(vSize) * 0.25;
    vec4 radii = vec4(float(vRadii.x & 127), float((vRadii.x >> 7) & 127),
                      float(vRadii.y & 127), float((vRadii.y >> 7) & 127)) * 0.5;

    bool shadow = vLine < 0.0;
    float blur = shadow ? -vLine : 0.0;
    vec2 shape = size - 2.0 * blur;                       // dla cienia quad jest powiększony o blur

    vec2 p = (vUv - 0.5) * size;
    float d = sdRoundBox(p, 0.5 * shape, radii);
    float aa = max(fwidth(d), 1e-4);                      // szerokość piksela w jednostkach odległości

    float cov;
    if (shadow) {
        cov = 1.0 - smoothstep(-0.5 * blur, 0.5 * blur, d);          // miękkie przejście o szerokości ~blur
    } else {
        cov = clamp(0.5 - d / aa, 0.0, 1.0);                          // dokładne pokrycie piksela = gładka krawędź
        if (vLine > 0.0) cov *= clamp(0.5 + (d + vLine) / aa, 0.0, 1.0);   // obrys: odetnij wnętrze
    }

    vec4 c = vec4(vColor.rgb, vColor.a * cov) * ColorModulator;
    if (c.a <= 0.002) discard;
    fragColor = c;
}
