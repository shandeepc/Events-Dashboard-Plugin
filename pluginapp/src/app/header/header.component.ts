import { Component } from '@angular/core';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  headerClass: string = '';
  ngOnInit() {
    // Check if bg-primary class exists in the document
    const testElement = document.createElement('div');
    testElement.className = 'bg-primary';
    document.body.appendChild(testElement);
    const hasBgPrimary = getComputedStyle(testElement).backgroundColor !== 'rgba(0, 0, 0, 0)';
    document.body.removeChild(testElement);

    this.headerClass = hasBgPrimary ? 'bg-primary' : 'edp-bg-primary';
  }
}
