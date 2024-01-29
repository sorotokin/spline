"use strict";

import { Spline } from "../dist/spline.js";

var main = document.getElementById("main");

var parametric = document.title.toLowerCase().indexOf("parametric") >= 0;

var path;
var selected;
var selectedCX0;
var selectedCY0;
var selectedX0;
var selectedY0;
var selectedMinX;
var selectedMaxX;

function updateSplinePath() {
    if (path && path.parentNode == main) {
        main.removeChild(path);
    }
    let x = [];
    let y = [];
    for (let child = main.firstElementChild; child; child = child.nextElementSibling) {
        if (child instanceof SVGCircleElement) {
            x.push(parseFloat(child.getAttribute("cx")));
            y.push(parseFloat(child.getAttribute("cy")));
        }
    }
    path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    path.setAttribute("fill", "none")
    path.setAttribute("stroke", "blue")
    if (parametric) {
        let t = [];
        for (let i = 0; i < x.length; i++) {
            t.push(i);
        }
        let splineX = new Spline(t, x);
        let splineY = new Spline(t, y);
        path.setAttribute("d", splineX.asPath2(splineY));

    } else {
        let spline = new Spline(x, y);
        path.setAttribute("d", spline.asPath());
    }
    main.insertBefore(path, main.firstChild);
}

function selectCircle(evt) {
    if (!(evt.target instanceof SVGCircleElement)) {
        return;
    }
    selected = evt.target;
    selected.setAttribute("stroke", "black");
    selectedCX0 = parseFloat(selected.getAttribute("cx"));
    selectedCY0 = parseFloat(selected.getAttribute("cy"));
    selectedX0 = evt.clientX;
    selectedY0 = evt.clientY;
    let prev = parametric ? null : selected.previousElementSibling;
    let next = parametric ? null : selected.nextElementSibling;
    selectedMinX = (prev instanceof SVGCircleElement ? parseFloat(prev.getAttribute("cx")) : 0) + 10;
    selectedMaxX = (next instanceof SVGCircleElement ? parseFloat(next.getAttribute("cx")) : window.innerWidth) - 10;
}

function moveCircle(evt) {
    if (selected) {
        let newX = selectedCX0 + (evt.clientX - selectedX0);
        if (newX >= selectedMinX && newX <= selectedMaxX) {
            selected.setAttribute("cx", newX);
            selected.setAttribute("cy", selectedCY0 + (evt.clientY - selectedY0));
            updateSplinePath();
        }
    }
}

function endSelection() {
    if (selected) {
        selected.setAttribute("stroke", "none");
    }
    selected = null;
}

main.addEventListener("mousedown", selectCircle);
main.addEventListener("mousemove", moveCircle);
main.addEventListener("mouseup", endSelection);

updateSplinePath();