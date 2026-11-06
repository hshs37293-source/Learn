#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 vUv;
layout(location = 1) in vec4 vColor;
layout(location = 2) in vec4 vColor2;
layout(location = 3) flat in vec4 vParams0;
layout(location = 4) flat in vec4 vRadii;
layout(location = 0) out vec4 color;

float sdBox(vec2 p, vec2 b, vec4 r) {
    // r: x=prawy-górny, y=prawy-dolny, z=lewy-dolny, w=lewy-górny; p.y rośnie ku górze
    vec2 rs = (p.x > 0.0) ? r.xy : r.wz;
    float rr = (p.y > 0.0) ? rs.x : rs.y;
    vec2 q = abs(p) - b + rr;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rr;
}

void main() {
    vec2 size = vParams0.xy;
    float bw = vParams0.z;
    vec2 p = (vUv - 0.5) * size;
    p.y = -p.y;                                   // UV rośnie w dół
    float d = sdBox(p, 0.5 * size, vRadii);
    float aa = max(fwidth(d), 1e-4);

    float shape = 1.0 - smoothstep(-aa, 0.0, d);
    vec4 fill = mix(vColor, vColor2, vUv.y);      // gradient pionowy
    float inner = (bw > 0.0) ? smoothstep(-bw - aa, -bw + aa, d) : 0.0;
    vec4 c = mix(fill, vColor2, inner);           // obrys w kolorze 2
    c.a *= shape;
    if (c.a <= 0.001) discard;
    color = c;
}
