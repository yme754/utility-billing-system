import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MyConnectionsComponent } from './my-connections';

describe('MyConnections', () => {
  let component: MyConnectionsComponent;
  let fixture: ComponentFixture<MyConnectionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyConnectionsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MyConnectionsComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
