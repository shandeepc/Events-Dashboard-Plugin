import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EventFullViewComponent } from './event-full-view.component';

describe('EventFullViewComponent', () => {
  let component: EventFullViewComponent;
  let fixture: ComponentFixture<EventFullViewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ EventFullViewComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EventFullViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
