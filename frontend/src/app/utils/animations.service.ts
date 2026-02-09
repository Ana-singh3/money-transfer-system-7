import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AnimationsService {
  fadeIn(element: HTMLElement | string, duration: number = 0.6, delay: number = 0): void {
    const el = typeof element === 'string' ? document.querySelector(element) as HTMLElement : element;
    if (el) {
      el.style.opacity = '0';
      el.style.transform = 'translateY(30px)';
      el.style.transition = `opacity ${duration}s ease-out, transform ${duration}s ease-out`;
      setTimeout(() => {
        el.style.opacity = '1';
        el.style.transform = 'translateY(0)';
      }, delay * 1000);
    }
  }

  slideIn(element: HTMLElement | string, direction: 'left' | 'right' | 'up' | 'down' = 'left', duration: number = 0.6): void {
    const el = typeof element === 'string' ? document.querySelector(element) as HTMLElement : element;
    if (!el) return;
    
    const directions = {
      left: { x: '-100px', y: '0' },
      right: { x: '100px', y: '0' },
      up: { x: '0', y: '-100px' },
      down: { x: '0', y: '100px' }
    };
    
    el.style.opacity = '0';
    el.style.transform = `translate(${directions[direction].x}, ${directions[direction].y})`;
    el.style.transition = `opacity ${duration}s ease-out, transform ${duration}s ease-out`;
    
    setTimeout(() => {
      el.style.opacity = '1';
      el.style.transform = 'translate(0, 0)';
    }, 0);
  }

  scaleIn(element: HTMLElement | string, duration: number = 0.5): void {
    const el = typeof element === 'string' ? document.querySelector(element) as HTMLElement : element;
    if (el) {
      el.style.opacity = '0';
      el.style.transform = 'scale(0.8)';
      el.style.transition = `opacity ${duration}s ease-out, transform ${duration}s ease-out`;
      setTimeout(() => {
        el.style.opacity = '1';
        el.style.transform = 'scale(1)';
      }, 0);
    }
  }

  countUp(element: HTMLElement, start: number, end: number, duration: number = 2): void {
    const startTime = Date.now();
    const range = end - start;
    
    const update = () => {
      const elapsed = (Date.now() - startTime) / 1000;
      const progress = Math.min(elapsed / duration, 1);
      const current = start + (range * progress);
      
      if (element) {
        element.textContent = current.toFixed(2);
      }
      
      if (progress < 1) {
        requestAnimationFrame(update);
      }
    };
    
    update();
  }

  staggerFadeIn(elements: HTMLElement[] | string, stagger: number = 0.1): void {
    const els = typeof elements === 'string' 
      ? Array.from(document.querySelectorAll(elements)) as HTMLElement[]
      : elements;
    
    els.forEach((el, index) => {
      el.style.opacity = '0';
      el.style.transform = 'translateY(20px)';
      el.style.transition = `opacity 0.6s ease-out ${index * stagger}s, transform 0.6s ease-out ${index * stagger}s`;
      
      setTimeout(() => {
        el.style.opacity = '1';
        el.style.transform = 'translateY(0)';
      }, index * stagger * 1000);
    });
  }
}
